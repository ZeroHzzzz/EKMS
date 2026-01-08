-- Repair script to fix multiple 'is_published' flags for a single knowledge document
-- It keeps only the latest version (by version number) as published

UPDATE knowledge_version kv
JOIN (
    SELECT knowledge_id, MAX(version) as max_version
    FROM knowledge_version
    WHERE is_published = 1
    GROUP BY knowledge_id
) latest ON kv.knowledge_id = latest.knowledge_id
SET kv.is_published = 0
WHERE kv.is_published = 1 
AND kv.version < latest.max_version;

-- Optional: Ensure every knowledge has at least one published version if the knowledge itself is APPROVED
-- (This part depends on specific business rules, usually the above cleanup is sufficient for the reported visual bug)
