package org.bert;

import org.bert.Bert.Atom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.rules.ExpectedException;
import java.util.Arrays;
import java.util.Date;

@RunWith(JUnit4.class)
public class BertEncodeTest {

	@Test
	public void encodeAtom() throws BertException {
		Bert.Atom atom = new Bert.Atom("demo");
		Bert bert = new Bert();

		byte[] data = bert.encode(atom);

		assert(data != null);

		byte[] erlang = { (byte) 131, 100, 0, 4, 100, 101, 109, 111 };

		assert(Arrays.equals(data, erlang));
	}

	@Test
	public void encodeBertTerm() throws BertException {
		Bert bert = new Bert();
		byte[] data = bert.encode(null);
		assert(data != null);

		byte[] n = { (byte) 131, 104, 2, 100, 0, 4, 98, 101, 114, 116, 100, 0, 3, 110, 105, 108 };
		assert(Arrays.equals(data, n));

		data = bert.encode(true);
		byte[] t = { (byte) 131, 104, 2, 100, 0, 4, 98, 101, 114, 116, 100, 0, 4, 116, 114, 117, 101 };
		assert(Arrays.equals(data, t));

		data = bert.encode(false);
		byte[] f = { (byte) 131, 104, 2, 100, 0, 4, 98, 101, 114, 116, 100, 0, 5, 102, 97, 108, 115, 101 };
		assert(Arrays.equals(data, f));
	}

	@Test
	public void encodeInt() throws BertException {
		Bert bert = new Bert();
		byte[] data = bert.encode(255);
		assert(data != null);

		byte[] small = { (byte) 131, (byte) 97, (byte) -1};
		assert(Arrays.equals(data, small));

		byte[] intv = { (byte) 131, (byte) 98, (byte) 255, (byte) 255, (byte) 255, (byte) 133 };
		data = bert.encode(-123);
		assert(Arrays.equals(data, intv));
	}
	
	@Test
	public void encodeFloat() throws BertException {
		Bert bert = new Bert();
		byte[] data = bert.encode(-1.23);

		byte[] fval = { (byte) 131, 99, 45, 49, 46, 50, 51, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 101, 43, 48, 48, 0, 0, 0, 0 };
		assert(Arrays.equals(data, fval));
	}

	@Test
	public void encodeErlangNil() throws BertException {
		Bert bert = new Bert();
		byte[] data = bert.encode(new Bert.List());

		byte[] nil = { (byte) 131, 106 };
		assert(Arrays.equals(data, nil));
	}

	@Test
	public void encodeString() throws BertException {
		Bert bert = new Bert();
		byte[] data = bert.encode("abc");

		byte[] abc = { (byte) 131, 107, 0, 3, 'a', 'b', 'c' };
		assert(Arrays.equals(data, abc));
	}

	@Test
	public void encodeBinary() throws BertException {
		Bert bert = new Bert();
		byte[] abc = { 'a', 'b', 'c' };
		byte[] data = bert.encode(abc);

		byte[] output = { (byte) 131, 109, 0, 0, 0, 3, 'a', 'b', 'c' };
		assert(Arrays.equals(data, output));
	}

}