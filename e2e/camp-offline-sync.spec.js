// @ts-check
const { test, expect } = require('@playwright/test');

test.describe('Phase 5: Medical Camp PWA Offline Registration & Idempotent Sync', () => {

  test('Goes offline, registers patient into IndexedDB queue, reconnects, syncs, and verifies idempotent backend persistence', async ({ context, page }) => {
    // Automatically accept any alerts/confirmations (such as sync confirmation)
    page.on('dialog', async dialog => {
      console.log(`[Browser Alert]: ${dialog.message()}`);
      await dialog.accept();
    });

    console.log('1. Loading Camp PWA at http://localhost:8080/camp-pwa.html');
    await page.goto('http://localhost:8080/camp-pwa.html');

    // Wait for the camp selector to be populated with a valid camp
    await page.waitForFunction(() => {
      const sel = document.querySelector('#activeCampSelect');
      return sel && sel.value && sel.value !== '';
    });
    const selectedCamp = await page.$eval('#activeCampSelect', el => el.value);
    expect(selectedCamp).toBeTruthy();
    console.log(`Active Camp Selected: ${selectedCamp}`);

    // Verify initial online state
    await expect(page.locator('#syncStatusText')).toHaveText('ONLINE');

    // Step 2: Simulate going offline
    console.log('2. Simulating network disconnect (going offline)...');
    await context.setOffline(true);
    await page.evaluate(() => window.dispatchEvent(new Event('offline')));
    await page.waitForTimeout(600);

    // Verify offline status reflected in UI
    await expect(page.locator('#syncStatusText')).toHaveText('OFFLINE');
    await expect(page.locator('#offlineBanner')).toHaveClass(/show/);
    console.log('Offline mode verified on UI.');

    // Step 3: Register a patient while offline (min 3 fields: Name, Phone, Age + Vitals)
    const testPhone = '+919822' + Math.floor(100000 + Math.random() * 900000);
    const testName = 'Shantabai Gawande';
    console.log(`3. Registering patient offline: ${testName} (${testPhone})...`);

    await page.fill('#regName', testName);
    await page.fill('#regPhone', testPhone);
    await page.fill('#regAge', '48');
    await page.selectOption('#regGender', 'FEMALE');
    await page.fill('#regVillage', 'Lohara');
    await page.selectOption('#regTriage', 'URGENT');
    await page.fill('#regBp', '142/92');
    await page.fill('#regPulse', '84');
    await page.fill('#regTemp', '99.1');
    await page.fill('#regSpo2', '97');
    await page.fill('#regSymptoms', 'Headache and fatigue');

    // Click the big green Register button
    await page.click('#btnRegister');
    await page.waitForTimeout(800);

    // Verify patient appears in the local attendee list with "Pending Sync" badge
    const attendeeList = page.locator('#attendeesList');
    await expect(attendeeList).toContainText(testName);
    await expect(attendeeList).toContainText('Pending Sync');

    // Verify pending sync counter is at least 1
    const pendingCount = await page.locator('#pendingCount').innerText();
    expect(parseInt(pendingCount, 10)).toBeGreaterThanOrEqual(1);
    console.log(`Offline registration saved to IndexedDB queue. Pending sync items: ${pendingCount}`);

    // Step 4: Reconnect to network (go online)
    console.log('4. Reconnecting to network (going online)...');
    await context.setOffline(false);
    await page.evaluate(() => window.dispatchEvent(new Event('online')));
    await page.waitForTimeout(600);

    await expect(page.locator('#syncStatusText')).toHaveText('ONLINE');
    console.log('Online status restored.');

    // Step 5: Trigger synchronization
    console.log('5. Triggering sync to backend...');
    await page.click('#btnSync');
    await page.waitForTimeout(1500);

    // After sync, pending count should become 0 and attendee badge should be "Synced"
    await expect(page.locator('#pendingCount')).toHaveText('0');
    await expect(attendeeList).toContainText(testName);
    await expect(attendeeList).toContainText('Synced');
    console.log('Sync completed and verified in UI.');

    // Step 6: Query backend API directly to verify persistence in PostgreSQL
    console.log('6. Querying backend REST API to verify attendee record in database...');
    const response = await page.request.get(`http://localhost:8080/api/camps/${selectedCamp}/attendees`);
    expect(response.ok()).toBeTruthy();
    const attendees = await response.json();

    const savedRecord = attendees.find(a => a.phone === testPhone);
    expect(savedRecord).toBeDefined();
    expect(savedRecord.fullName).toBe(testName);
    expect(savedRecord.vitalBp).toBe('142/92');
    expect(savedRecord.triageLevel).toBe('URGENT');
    console.log(`Verified record in database: ${savedRecord.fullName}, Sync ID: ${savedRecord.clientSyncId}`);

    // Step 7: Verify Idempotent Deduplication
    console.log('7. Testing idempotent sync deduplication with duplicate clientSyncId...');
    const duplicatePayload = {
      campId: selectedCamp,
      registrations: [
        {
          id: savedRecord.id,
          clientSyncId: savedRecord.clientSyncId,
          fullName: testName,
          phone: testPhone,
          age: 48,
          gender: 'FEMALE',
          village: 'Lohara',
          triageLevel: 'URGENT',
          registeredOffline: true
        }
      ],
      services: [],
      vaccinations: [],
      dispenses: []
    };

    const dupResponse = await page.request.post(`http://localhost:8080/api/camps/${selectedCamp}/sync`, {
      data: duplicatePayload
    });
    expect(dupResponse.ok()).toBeTruthy();
    const dupResult = await dupResponse.json();

    expect(dupResult.status).toBe('SUCCESS');
    expect(dupResult.syncedRegistrations).toBe(0);
    expect(dupResult.duplicateRegistrations).toBe(1);
    console.log(`✓ Idempotence verified: 0 new inserted, 1 duplicate safely ignored.`);
  });

  test('Equity Map loads villages and hospitals, and next-camp recommender pre-fills camp creation', async ({ page }) => {
    page.on('dialog', async dialog => await dialog.accept());

    await page.goto('http://localhost:8080/camp-pwa.html');

    // Switch to Equity tab
    await page.click('button:has-text("GIS Equity Map")');
    await page.waitForTimeout(1000);

    // Verify Leaflet map container is initialized
    await expect(page.locator('#equityMap')).toBeVisible();

    // Verify recommendations leaderboard has items
    await page.waitForSelector('#recommenderList .rec-item');
    const recCount = await page.locator('#recommenderList .rec-item').count();
    expect(recCount).toBeGreaterThanOrEqual(3);

    // Click Deploy on the top recommended village
    console.log('Testing next-camp recommender pre-fill...');
    await page.locator('#recommenderList .rec-item').first().locator('button:has-text("Deploy")').click();

    // Verify modal opened with pre-filled inputs
    await expect(page.locator('#newCampModal')).toBeVisible();
    const prefilledName = await page.locator('#modalCampName').inputValue();
    const prefilledVillage = await page.locator('#modalVillageName').inputValue();
    const prefilledVenue = await page.locator('#modalVenue').inputValue();
    const prefilledLat = await page.locator('#modalLat').inputValue();
    const prefilledLng = await page.locator('#modalLng').inputValue();

    expect(prefilledName).toContain('Camp');
    expect(prefilledVillage).toBeTruthy();
    expect(prefilledVenue).toContain('Zilla Parishad');
    expect(parseFloat(prefilledLat)).toBeGreaterThan(19.0);
    expect(parseFloat(prefilledLng)).toBeGreaterThan(77.0);

    console.log(`✓ Pre-filled recommendation verified for: ${prefilledVillage} at [${prefilledLat}, ${prefilledLng}]`);

    // Close modal
    await page.click('#newCampModal button:has-text("✕")');
    await expect(page.locator('#newCampModal')).not.toBeVisible();
  });
});

