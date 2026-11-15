import { spawnSync } from 'node:child_process';

const env = { ...process.env, PGPASSWORD: 'aniket@12345' };
const res = spawnSync('psql', [
  '-U', 'postgres',
  '-h', 'localhost',
  '-d', 'email_forensics',
  '-c', "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
], { env, encoding: 'utf8' });

console.log(res.stdout || res.stderr);
