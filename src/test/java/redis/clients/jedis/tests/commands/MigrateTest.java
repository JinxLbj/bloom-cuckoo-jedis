package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.CuckooJedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.MigrateParams;

public class MigrateTest extends CuckooJedisCommandTestBase {

  private static final byte[] bfoo = {0x01, 0x02, 0x03};
  private static final byte[] bbar = {0x04, 0x05, 0x06};
  private static final byte[] bfoo1 = {0x07, 0x08, 0x01};
  private static final byte[] bbar1 = {0x09, 0x00, 0x01};
  private static final byte[] bfoo2 = {0x07, 0x08, 0x02};
  private static final byte[] bbar2 = {0x09, 0x00, 0x02};
  private static final byte[] bfoo3 = {0x07, 0x08, 0x03};
  private static final byte[] bbar3 = {0x09, 0x00, 0x03};

  private CuckooJedis dest;
  private CuckooJedis destAuth;
  private static final String host = hnp.getHost();
  private static final int port = 6386;
  private static final int portAuth = hnp.getPort() + 1;
  private static final int db = 2;
  private static final int dbAuth = 3;
  private static final int timeout = Protocol.DEFAULT_TIMEOUT;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    dest = new CuckooJedis(host, port, 500);
    dest.flushAll();
    dest.select(db);

    destAuth = new CuckooJedis(host, portAuth, 500);
    destAuth.auth("foobared");
    destAuth.flushAll();
    destAuth.select(dbAuth);
  }

  @After
  @Override
  public void tearDown() {
    dest.close();
    destAuth.close();
    super.tearDown();
  }

  @Test
  public void nokey() {
    assertEquals("NOKEY", cuckooJedis.migrate(host, port, "foo", db, timeout));
    assertEquals("NOKEY", cuckooJedis.migrate(host, port, bfoo, db, timeout));
    assertEquals("NOKEY", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("NOKEY", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
  }

  @Test
  public void migrate() {
    cuckooJedis.set("foo", "bar");
    assertEquals("OK", cuckooJedis.migrate(host, port, "foo", db, timeout));
    assertEquals("bar", dest.get("foo"));
    assertNull(cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar);
    assertEquals("OK", cuckooJedis.migrate(host, port, bfoo, db, timeout));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertNull(cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateEmptyParams() {
    cuckooJedis.set("foo", "bar");
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), "foo"));
    assertEquals("bar", dest.get("foo"));
    assertNull(cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar);
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertNull(cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateCopy() {
    cuckooJedis.set("foo", "bar");
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().copy(), "foo"));
    assertEquals("bar", dest.get("foo"));
    assertEquals("bar", cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar);
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().copy(), bfoo));
    assertArrayEquals(bbar, dest.get(bfoo));
    assertArrayEquals(bbar, cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateReplace() {
    cuckooJedis.set("foo", "bar1");
    dest.set("foo", "bar2");
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().replace(), "foo"));
    assertEquals("bar1", dest.get("foo"));
    assertNull(cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar1);
    dest.set(bfoo, bbar2);
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().replace(), bfoo));
    assertArrayEquals(bbar1, dest.get(bfoo));
    assertNull(cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateCopyReplace() {
    cuckooJedis.set("foo", "bar1");
    dest.set("foo", "bar2");
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), "foo"));
    assertEquals("bar1", dest.get("foo"));
    assertEquals("bar1", cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar1);
    dest.set(bfoo, bbar2);
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams().copy().replace(), bfoo));
    assertArrayEquals(bbar1, dest.get(bfoo));
    assertArrayEquals(bbar1, cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateAuth() {
    cuckooJedis.set("foo", "bar");
    assertEquals("OK", cuckooJedis.migrate(host, portAuth, dbAuth, timeout, new MigrateParams().auth("foobared"), "foo"));
    assertEquals("bar", destAuth.get("foo"));
    assertNull(cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar);
    assertEquals("OK", cuckooJedis.migrate(host, portAuth, dbAuth, timeout, new MigrateParams().auth("foobared"), bfoo));
    assertArrayEquals(bbar, destAuth.get(bfoo));
    assertNull(cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateCopyReplaceAuth() {
    cuckooJedis.set("foo", "bar1");
    destAuth.set("foo", "bar2");
    assertEquals("OK", cuckooJedis.migrate(host, portAuth, dbAuth, timeout, new MigrateParams().copy().replace().auth("foobared"), "foo"));
    assertEquals("bar1", destAuth.get("foo"));
    assertEquals("bar1", cuckooJedis.get("foo"));

    cuckooJedis.set(bfoo, bbar1);
    destAuth.set(bfoo, bbar2);
    assertEquals("OK", cuckooJedis.migrate(host, portAuth, dbAuth, timeout, new MigrateParams().copy().replace().auth("foobared"), bfoo));
    assertArrayEquals(bbar1, destAuth.get(bfoo));
    assertArrayEquals(bbar1, cuckooJedis.get(bfoo));
  }

  @Test
  public void migrateMulti() {
    cuckooJedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3"));
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar2", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    cuckooJedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    assertEquals("OK", cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3));
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar2, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

  @Test
  public void migrateConflict() {
    cuckooJedis.mset("foo1", "bar1", "foo2", "bar2", "foo3", "bar3");
    dest.set("foo2", "bar");
    try {
      cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), "foo1", "foo2", "foo3");
      fail("Should get BUSYKEY error");
    } catch(JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertEquals("bar1", dest.get("foo1"));
    assertEquals("bar", dest.get("foo2"));
    assertEquals("bar3", dest.get("foo3"));

    cuckooJedis.mset(bfoo1, bbar1, bfoo2, bbar2, bfoo3, bbar3);
    dest.set(bfoo2, bbar);
    try {
      cuckooJedis.migrate(host, port, db, timeout, new MigrateParams(), bfoo1, bfoo2, bfoo3);
      fail("Should get BUSYKEY error");
    } catch(JedisDataException jde) {
      assertTrue(jde.getMessage().contains("BUSYKEY"));
    }
    assertArrayEquals(bbar1, dest.get(bfoo1));
    assertArrayEquals(bbar, dest.get(bfoo2));
    assertArrayEquals(bbar3, dest.get(bfoo3));
  }

}
