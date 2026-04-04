-- Add search_vector column
ALTER TABLE courses ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create trigger function logic
CREATE OR REPLACE FUNCTION courses_search_trigger() RETURNS trigger AS $$
begin
  new.search_vector :=
    setweight(to_tsvector('english', coalesce(new.title,'')), 'A') ||
    setweight(to_tsvector('english', coalesce(new.description,'')), 'B');
return new;
end
$$ LANGUAGE plpgsql;

-- Create trigger (for future inserts/updates)
DROP TRIGGER IF EXISTS tg_courses_search_update ON courses;
CREATE TRIGGER tg_courses_search_update
    BEFORE INSERT OR UPDATE ON courses
                         FOR EACH ROW EXECUTE FUNCTION courses_search_trigger();

-- Update existing rows (for old data)
UPDATE courses SET search_vector =
                       setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
                       setweight(to_tsvector('english', coalesce(description, '')), 'B');

-- Create index for faster search
CREATE INDEX IF NOT EXISTS idx_courses_search_vector ON courses USING GIN(search_vector);