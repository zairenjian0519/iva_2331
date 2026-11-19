SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO kyscene_tm2.ky_category
    (id, name, code, pid, category, has_child, create_time, threshold,
     target_threshold, algoid, algoname, alamid, hit_count, iou_threshold,
     sensitivity, time_space)
SELECT CAST(UUID_SHORT() AS CHAR), '车辆检测', '18', '1562691699415764993',
       '0', '0', NOW(), 50, 0, NULL, NULL, NULL, 5, 70, 2, 4
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM kyscene_tm2.ky_category WHERE code = '18'
);

INSERT INTO kyscene_tm2.alarm_type_refer
    (alarm_code, alarm_name, new_alarm_code, new_alarm_name)
SELECT '18', '车辆检测', NULL, NULL
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM kyscene_tm2.alarm_type_refer WHERE alarm_code = '18'
);

COMMIT;

SELECT id, name, code, pid, threshold, hit_count, iou_threshold
FROM kyscene_tm2.ky_category
WHERE code = '18';

SELECT alarm_code, alarm_name
FROM kyscene_tm2.alarm_type_refer
WHERE alarm_code = '18';
