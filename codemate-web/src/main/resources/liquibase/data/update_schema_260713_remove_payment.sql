DROP TABLE IF EXISTS `article_pay_record`;

ALTER TABLE `article`
    DROP COLUMN `pay_amount`,
    DROP COLUMN `pay_way`;

ALTER TABLE `user_info`
    DROP COLUMN `pay_code`;
