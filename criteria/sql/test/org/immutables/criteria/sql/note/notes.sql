DROP TABLE IF EXISTS notes;
CREATE TABLE notes (
  id VARCHAR NOT NULL PRIMARY KEY,
  created_on BIGINT NOT NULL,
  message VARCHAR(255) NOT NULL
);
INSERT INTO notes (id, created_on, message)
VALUES('7aae20e3-9144-40ba-86f0-c9de8f143269',1640995200000,'Message #1'),
      ('75b90525-38be-41b9-b43d-df427a66893c',1609459200000,'Message #2'),
      ('578f932f-c972-4d3b-92b4-bf8fda9599f9',1577836800000,'Message #3'),
      ('cf508c17-8217-4f30-a99f-70fdfbcdc643',1546300800000,'Message #4'),
      ('c49371f3-8cda-4ddf-ad74-877ee6f67abe',0,'Message #5');
