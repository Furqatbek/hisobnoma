-- Translate default UOM names and descriptions to Uzbek Cyrillic
UPDATE units_of_measure SET name = 'Дона', description = 'Якка дона ёки бирлик' WHERE code = 'PCS' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Килограмм', description = 'Оғирлик (килограмм)' WHERE code = 'KG' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Грамм', description = 'Оғирлик (грамм)' WHERE code = 'G' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Литр', description = 'Ҳажм (литр)' WHERE code = 'L' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Миллилитр', description = 'Ҳажм (миллилитр)' WHERE code = 'ML' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Метр', description = 'Узунлик (метр)' WHERE code = 'M' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Сантиметр', description = 'Узунлик (сантиметр)' WHERE code = 'CM' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Қути', description = 'Қути қадоқ' WHERE code = 'BOX' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Пачка', description = 'Пачка ёки тўплам' WHERE code = 'PACK' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Тўплам', description = 'Буюмлар тўплами' WHERE code = 'SET' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Жуфт', description = 'Жуфт буюмлар' WHERE code = 'PAIR' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Дюжина', description = '12 дона' WHERE code = 'DOZEN' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Рулон', description = 'Рулон қадоқ' WHERE code = 'ROLL' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Шиша', description = 'Шиша қадоқ' WHERE code = 'BTL' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Банка', description = 'Банка қадоқ' WHERE code = 'CAN' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Қоп', description = 'Қоп қадоқ' WHERE code = 'BAG' AND tenant_id = 1;
UPDATE units_of_measure SET name = 'Бирлик', description = 'Умумий бирлик' WHERE code = 'UNIT' AND tenant_id = 1;
