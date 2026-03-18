INSERT INTO recommendation_catalog (category_id, service_name, alternative_service, reason, score)
SELECT c.id, 'Netflix', 'Amazon Prime Video', 'Lower annual cost and bundled shipping benefits', 95
FROM categories c
WHERE c.name = 'Entertainment'
  AND NOT EXISTS (
      SELECT 1
      FROM recommendation_catalog rc
      WHERE rc.category_id = c.id
        AND rc.service_name = 'Netflix'
        AND rc.alternative_service = 'Amazon Prime Video'
  );

INSERT INTO recommendation_catalog (category_id, service_name, alternative_service, reason, score)
SELECT c.id, 'YouTube Premium', 'Spotify Premium', 'Music-focused plan with lower monthly fee', 88
FROM categories c
WHERE c.name = 'Entertainment'
  AND NOT EXISTS (
      SELECT 1
      FROM recommendation_catalog rc
      WHERE rc.category_id = c.id
        AND rc.service_name = 'YouTube Premium'
        AND rc.alternative_service = 'Spotify Premium'
  );

INSERT INTO recommendation_catalog (category_id, service_name, alternative_service, reason, score)
SELECT c.id, 'Notion', 'Microsoft To Do', 'Simpler task management for lower recurring spend', 82
FROM categories c
WHERE c.name = 'Productivity'
  AND NOT EXISTS (
      SELECT 1
      FROM recommendation_catalog rc
      WHERE rc.category_id = c.id
        AND rc.service_name = 'Notion'
        AND rc.alternative_service = 'Microsoft To Do'
  );

INSERT INTO recommendation_catalog (category_id, service_name, alternative_service, reason, score)
SELECT c.id, 'Dropbox', 'Google One', 'Comparable cloud storage with better family pricing', 84
FROM categories c
WHERE c.name = 'Cloud'
  AND NOT EXISTS (
      SELECT 1
      FROM recommendation_catalog rc
      WHERE rc.category_id = c.id
        AND rc.service_name = 'Dropbox'
        AND rc.alternative_service = 'Google One'
  );
