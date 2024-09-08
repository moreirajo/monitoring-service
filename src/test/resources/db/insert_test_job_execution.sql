insert into job_execution (id, external_id, job_name, url, status, response_time, error_message, created_date,
                           last_modified_date, created_by, last_modified_by, trace_id)
values (1, 'edd24f58-81f7-4ee8-91b5-683cd6ccdef6', 'google', 'https://www.google.com', 'SUCCEEDED',
        80,
        null, '2023-01-01 12:00:00.000000', '2023-01-01 12:00:00.000000', 'test-user', 'test-user',
        '00000000000000000000000000000000');

insert into job_execution (id, external_id, job_name, url, status, response_time, error_message, created_date,
                           last_modified_date, created_by, last_modified_by, trace_id)
values (2, '0bb1165f-2860-44d1-ae6e-b31f0b6f2cfa', 'google', 'https://www.google.com', 'SUCCEEDED',
        82,
        null, '2023-01-01 12:01:00.000000', '2023-01-01 12:01:00.000000', 'test-user', 'test-user',
        '00000000000000000000000000000000');

insert into job_execution (id, external_id, job_name, url, status, response_time, error_message, created_date,
                           last_modified_date, created_by, last_modified_by, trace_id)
values (3, '800d8f15-3133-44f4-9a63-63af5bc1c96e', 'google fake', 'https://www.google.com/fake', 'FAILED',
        89,
        '404 not found', '2023-01-01 12:03:00.000000', '2023-01-01 12:03:00.000000', 'test-user', 'test-user',
        '00000000000000000000000000000000');
