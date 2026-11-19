import assert from 'assert';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const source = readFileSync(process.argv[2], 'utf8');
for (const host of ['192.168.1.66', '192.168.16.163']) {
  const window = { location: { hostname: host } };
  runInNewContext(source, { window });
  assert.strictEqual(
    window.__PRODUCTION__JEECGBOOTADMIN__CONF__.VITE_GLOB_DOMAIN_URL,
    `http://${host}:8088/kyscene`,
  );
}
