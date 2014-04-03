package org.bert;

import java.util.ArrayList;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Bert {

	private byte[] mFloatStr = new byte[31];
	private ByteBuffer mBuffer = null;
	private Object mValue = null;

	public static class Atom {
		public String name;

		public Atom() {
		}

		public int hashCode() {
			return name.hashCode();
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof Atom)) return false;
			return name.compareTo(((Atom) obj).name) == 0;
		}

		public String toString() {
			return name;
		}
	}

	public static class Tuple extends ArrayList<Object> {
	}

	public static class List extends ArrayList<Object> {
		public boolean isProper = true;
	}

	public static class Dict extends HashMap<Object, Object> {
	}

	public Bert() {
	}

	public Bert(final byte[] data) throws BertException {
		mBuffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

		byte value = mBuffer.get();
		if (value != -125)
		 	throw new BertException("Invalid Bert Data");

		mValue = decode();
	}

	private Object decodeBertTerm(Tuple t) throws BertException {
		if (t.get(0) instanceof Atom && ((Atom) t.get(0)).name.compareTo("bert") ==  0) {
			if (t.size() == 5) {
				if (t.get(0) instanceof Atom && t.get(1) instanceof Atom &&
					((Atom) t.get(0)).name.compareTo("bert") == 0 &&
					((Atom) t.get(1)).name.compareTo("time") == 0 &&
					t.get(2) instanceof Integer &&
					t.get(3) instanceof Integer &&
					t.get(4) instanceof Integer) {
					long time = ((int) t.get(2) * (long) 1000000 * (long) 1000) + ((int) t.get(3) * (long) 1000) + ((int) t.get(4) / 1000);

					return time;
				}
			} else if (t.size() == 2) {
				String v = ((Atom) t.get(1)).name;
				if (v.compareTo("nil") == 0) {
					return null;
				} else if (v.compareTo("true") == 0) {
					return true;
				} else if (v.compareTo("false") == 0) {
					return false;
				}
			} else if (t.size() == 3) {
				if (t.get(0) instanceof Atom && t.get(1) instanceof Atom &&
					((Atom) t.get(0)).name.compareTo("bert") == 0 &&
					((Atom) t.get(1)).name.compareTo("dict") == 0 &&
					t.get(2) instanceof List) {
					Dict d = new Dict();
					List l = (List) t.get(2);

					for (int count = 0; count < l.size(); count++) {
						Tuple tup = (Tuple) l.get(count);
						if (tup.size() != 2)
							throw new BertException("Invalid Dict Entry");
						d.put(tup.get(0), tup.get(1));
					}

					return d;
				}
			}
		}

		return t;
	}

	private Object decodeSmallTuple() throws BertException {
		int len = mBuffer.get() & 0x00FFFFFFFF;

		Tuple tuple = new Tuple();
		for (int count = 0; count < len; count++) {
			tuple.add(decode());
		}

		return decodeBertTerm(tuple);
	}

	private Object decodeLargeTuple() throws BertException {
		int len = mBuffer.getInt() & 0x00FF;

		Tuple tuple = new Tuple();
		for (int count = 0; count < len; count++) {
			tuple.add(decode());
		}

		return decodeBertTerm(tuple);
	}

	public List decodeList() throws BertException {
		int len = mBuffer.getInt() & 0x00FF;

		List list = new List();
		for (int count = 0; count < len; count++) {
			list.add(decode());
		}

		Object o = decode();
		if (!(o instanceof List)) {
			list.add(o);
			list.isProper = false;
		}

		return list;
	}

	private Object decode() throws BertException {
		int tag = mBuffer.get() & 0x00FF;
		byte[] val = null;
		long len = 0;

		switch (tag) {
		case 97:  // SmallInt Tag
			return (int) (mBuffer.get() & 0x00FF);
		case 98:  // Int Tag
			return mBuffer.getInt();
		case 99:  // FloatTag
			mBuffer.get(mFloatStr);
			return Double.parseDouble(new String(mFloatStr));
		case 100: // AtomTag
			len = mBuffer.getShort() & 0x00FFFF;
			val = new byte[(int) len];
			mBuffer.get(val);
			Atom atom = new Atom();
			atom.name = new String(val);
			return atom;
		case 104: // SmallTupleTag
			return decodeSmallTuple();
		case 105: // LargeTupleTag
			return decodeLargeTuple();
		case 106: // NilTag
			return new List();
		case 107: // StringTag
			len = mBuffer.getShort() & 0x00FFFF;
			val = new byte[(int) len];
			mBuffer.get(val);
			return new String(val);
		case 108: // ListTag
			return decodeList();
		case 109: // BinTag
			len = mBuffer.getInt() & 0x00FFFFFFFF;
			val = new byte[(int) len];
			mBuffer.get(val);
			return val;
		default:
			throw new BertException("Not Supported Bert Tag");
		}
	}

	public Object getValue() {
		return mValue;
	}

	public String toString() {
		return mValue == null ? null : mValue.toString();
	}

}
