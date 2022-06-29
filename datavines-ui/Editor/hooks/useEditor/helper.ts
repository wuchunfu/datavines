import { THintsItem } from '../../type';

export const getTableColumnHints = (hints: THintsItem[], tableName = ''): string[] => {
    let arr: string[] = [];
    (hints || []).forEach((item) => {
        if (!tableName) {
            arr.push(item[0]);
            return;
        }
        if (item[0] === tableName) {
            arr = [...arr, ...(arr[1] || [])];
        }
    });
    return arr;
};

export const arrayRemoveRepeat = (arr: string[]) => [...new Set(arr || [])];

export const HINTS = [
    'SELECT',
    'INSERT',
    'DELETE',
    'UPDATE',
    'CREATE TABLE',
    'DROP TABLE',
    'ALTER TABLE',
    'CREATE VIEW',
    'DROP VIEW',
    'CREATE INDEX',
    'DROP INDEX',
    'CREATE PROCEDURE',
    'DROP PROCEDURE',
    'CREATE TRIGGER',
    'DROP TRIGGER',
    'CREATE SCHEMA',
    'DROP SCHEMA',
    'CREATE DOMAIN',
    'ALTER DOMAIN',
    'DROP DOMAIN',
    'GRANT',
    'DENY',
    'REVOKE',
    'COMMIT',
    'ROLLBACK',
    'SET TRANSACTION',
    'DECLARE',
    'EXPLAN',
    'OPEN',
    'FETCH',
    'CLOSE',
    'PREPARE',
    'EXECUTE',
    'DESCRIBE',
    'ORDER BY',
];

export const SQL_STRING = [
    'ADD',
    'EXCEPT',
    'PERCENT',
    'ALL',
    'EXEC',
    'PLAN',
    'ALTER',
    'EXECUTE',
    'PRECISION',
    'AND',
    'EXISTS',
    'PRIMARY',
    'ANY',
    'EXIT',
    'PRINT',
    'AS',
    'FETCH',
    'PROC',
    'ASC',
    'FILE',
    'PROCEDURE',
    'AUTHORIZATION',
    'FILLFACTOR',
    'PUBLIC',
    'BACKUP',
    'FOR',
    'RAISERROR',
    'BEGIN',
    'FOREIGN',
    'READ',
    'BETWEEN',
    'FREETEXT',
    'READTEXT',
    'BREAK',
    'FREETEXTTABLE',
    'RECONFIGURE',
    'BROWSE',
    'FROM',
    'REFERENCES',
    'BULK',
    'FULL',
    'REPLICATION',
    'BY',
    'FUNCTION',
    'RESTORE',
    'CASCADE',
    'GOTO',
    'RESTRICT',
    'CASE',
    'GRANT',
    'RETURN',
    'CHECK',
    'GROUP',
    'REVOKE',
    'CHECKPOINT',
    'HAVING',
    'RIGHT',
    'CLOSE',
    'HOLDLOCK',
    'ROLLBACK',
    'CLUSTERED',
    'IDENTITY',
    'ROWCOUNT',
    'COALESCE',
    'IDENTITY_INSERT',
    'ROWGUIDCOL',
    'COLLATE',
    'IDENTITYCOL',
    'RULE',
    'COLUMN',
    'IF',
    'SAVE',
    'COMMIT',
    'IN',
    'SCHEMA',
    'COMPUTE',
    'INDEX',
    'SELECT',
    'CONSTRAINT',
    'INNER',
    'SESSION_USER',
    'CONTAINS',
    'INSERT',
    'SET',
    'CONTAINSTABLE',
    'INTERSECT',
    'SETUSER',
    'CONTINUE',
    'INTO',
    'SHUTDOWN',
    'CONVERT',
    'IS',
    'SOME',
    'CREATE',
    'JOIN',
    'STATISTICS',
    'CROSS',
    'KEY',
    'SYSTEM_USER',
    'CURRENT',
    'KILL',
    'TABLE',
    'CURRENT_DATE',
    'LEFT',
    'TEXTSIZE',
    'CURRENT_TIME',
    'LIKE',
    'THEN',
    'CURRENT_TIMESTAMP',
    'LINENO',
    'TO',
    'CURRENT_USER',
    'LOAD',
    'TOP',
    'CURSOR',
    'NATIONAL',
    'TRAN',
    'DATABASE',
    'NOCHECK',
    'TRANSACTION',
    'DBCC',
    'NONCLUSTERED',
    'TRIGGER',
    'DEALLOCATE',
    'NOT',
    'TRUNCATE',
    'DECLARE',
    'NULL',
    'TSEQUAL',
    'DEFAULT',
    'NULLIF',
    'UNION',
    'DELETE',
    'OF',
    'UNIQUE',
    'DENY',
    'OFF',
    'UPDATE',
    'DESC',
    'OFFSETS',
    'UPDATETEXT',
    'DISK',
    'ON',
    'USE',
    'DISTINCT',
    'OPEN',
    'USER',
    'DISTRIBUTED',
    'OPENDATASOURCE',
    'VALUES',
    'DOUBLE',
    'OPENQUERY',
    'VARYING',
    'DROP',
    'OPENROWSET',
    'VIEW',
    'DUMMY',
    'OPENXML',
    'WAITFOR',
    'DUMP',
    'OPTION',
    'WHEN',
    'ELSE',
    'OR',
    'WHERE',
    'END',
    'ORDER',
    'WHILE',
    'ERRLVL',
    'OUTER',
    'WITH',
    'ESCAPE',
    'OVER',
    'WRITETEXT',
];