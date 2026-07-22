ALTER TABLE `ai_task_plan_step`
    ADD COLUMN `skipped_reason` text COMMENT '跳过原因' AFTER `blocked_reason`;
