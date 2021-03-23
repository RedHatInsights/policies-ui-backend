/* If actions contains 'notification' or 'email' set 'notification' */

UPDATE policy SET actions = 'notification' WHERE id IN (
  SELECT id FROM policy WHERE actions ~ '.*notification.*|.*email.*'
);

/* Remove old webhook action */

UPDATE policy SET actions = '' WHERE id IN (
  SELECT id FROM policy WHERE actions ~ '.*webhook.*'
);
