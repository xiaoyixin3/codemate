ALTER TABLE `ai_task_plan`
    ADD COLUMN `plan_key` varchar(64) DEFAULT NULL COMMENT '模型规划幂等键' AFTER `source_ai_type`,
    ADD UNIQUE KEY `uk_user_id_plan_key` (`user_id`, `plan_key`);
