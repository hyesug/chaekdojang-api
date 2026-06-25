UPDATE reading_groups
SET join_policy = 'APPROVAL',
    updated_at = CURRENT_TIMESTAMP
WHERE visibility = 'PRIVATE'
  AND join_policy <> 'APPROVAL';
