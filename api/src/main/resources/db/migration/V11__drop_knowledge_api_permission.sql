-- knowledge-api (kb.jorisjonkers.dev) was retired with the old knowledge stack;
-- Hindsight replaced it under the MEMORY_* permissions. The ServicePermission
-- enum drops KNOWLEDGE_API in the same release, so remove every row that names
-- it: a stranded user_service_permissions row is only skipped with a warning,
-- but a stranded service_tokens row fails to load and 500s the owner's token list.
DELETE FROM user_service_permissions WHERE service = 'KNOWLEDGE_API';
DELETE FROM service_tokens WHERE service = 'KNOWLEDGE_API';
