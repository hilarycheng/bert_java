package org.bert;

import java.util.ArrayList;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Bert {

	private byte[] mFloatStr = new byte[31];
	private ByteBuffer mBuffer = null;
	private ByteArrayOutputStream bao = null;
	private Object mValue = null;

	public static class Atom {
		public String name;

		public Atom(String name) {
			this.name = name;
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

	public static class Time {

		public Time() {
		}

		public Time(long ts) {
			timestamp = ts;

			microsecond = (int) ((ts % 1000) * 1000);
			second = (int) ((ts / 1000) % 1000000);
			megasecond = (int) ((ts / 1000) / 1000000);
		}

		public long timestamp = 0;

		public int megasecond  = 0;
		public int second      = 0;
		public int microsecond = 0;
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

	private void writeAtom(Atom a, ByteArrayOutputStream bao) throws BertException {
		int len = a.name.length();
		if (len >= 65536) throw new BertException("Atom Name too Long");
		bao.write(100);
		bao.write((byte) (len >> 8) & 0x00FF);
		bao.write((byte) (len     ) & 0x00FF);
		try {
			bao.write(a.name.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException ex) {
			throw new BertException("ISO 8859-1 is not Supported at Your Java Environment");
		} catch (IOException ex) {
			throw new BertException(ex.getMessage());
		}
	}

	private void writeTuple(Tuple tuple) throws BertException {
		int len = tuple.size();

		if (len < 256) {
			bao.write(104);
			bao.write((byte) (len & 0x00FF));
		} else {
			bao.write(105);
			bao.write((byte) ((len >> 24) & 0x00FF));
			bao.write((byte) ((len >> 16) & 0x00FF));
			bao.write((byte) ((len >>  8) & 0x00FF));
			bao.write((byte) ((len      ) & 0x00FF));
		}

		for (int count = 0; count < tuple.size(); count++) {
			encodeTerm(tuple.get(count));
		}
	}

	private void writeList(List list) throws BertException {
		int len = list.size();

		bao.write(108);
		bao.write((byte) ((len >> 24) & 0x00FF));
		bao.write((byte) ((len >> 16) & 0x00FF));
		bao.write((byte) ((len >>  8) & 0x00FF));
		bao.write((byte) ((len      ) & 0x00FF));

		for (int count = 0; count < list.size(); count++) {
			encodeTerm(list.get(count));
		}

		if (list.isProper) bao.write(106);
	}

	private void encodeTerm(Object o) throws BertException {

		if (o == null) {
			Atom bert = new Atom("bert");
			Atom nil = new Atom("nil");
			Tuple tup = new Tuple();
			tup.add(bert);
			tup.add(nil);
			writeTuple(tup);
		} else if (o instanceof Boolean) {
			Atom bert = new Atom("bert");
			Atom nil = new Atom((boolean) o ? "true" : "false");
			Tuple tup = new Tuple();
			tup.add(bert);
			tup.add(nil);
			writeTuple(tup);
		} else if (o instanceof Integer) {
			int value = (int) o;
			if (value >= 0 && value <= 255) {
				bao.write(97);
				bao.write((byte) (value & 0x00FF));
			} else {
				bao.write(98);
				bao.write((byte) ((value >> 24) & 0x00FF));
				bao.write((byte) ((value >> 16) & 0x00FF));
				bao.write((byte) ((value >>  8) & 0x00FF));
				bao.write((byte) ((value      ) & 0x00FF));
			}
		} else if (o instanceof Double || o instanceof Float) {
			double d = (double) o;
			byte[] val = String.format("%.20e", o).getBytes();
			try {
				bao.write(99);
				bao.write(val);
				if (val.length < 31) {
					for (int count = 0; count < 31 - val.length; count++) bao.write(0);
				}
			} catch (IOException ex) {
				throw new BertException(ex.getMessage());
			}
		} else if (o instanceof List) {
			List list = (List) o;
			if (list.size() == 0) {
				bao.write(106);
			} else {
				writeList((List) o);
			}
		} else if (o instanceof String) {
			try {
				byte[] str = ((String) o).getBytes("UTF-8");
				bao.write(107);
				bao.write((byte) ((str.length >> 8) & 0x00FF));
				bao.write((byte) ((str.length     ) & 0x00FF));
				bao.write(str);
			} catch (UnsupportedEncodingException ex) {
				new BertException("String not in UTF-8");
			} catch (IOException ex) {
				new BertException(ex.getMessage());
			}
		} else if (o instanceof Atom) {
			writeAtom((Atom) o, bao);
		} else if (o instanceof byte[]) {
			int value = ((byte[]) o).length;
			bao.write(109);
			bao.write((byte) ((value >> 24) & 0x00FF));
			bao.write((byte) ((value >> 16) & 0x00FF));
			bao.write((byte) ((value >>  8) & 0x00FF));
			bao.write((byte) ((value      ) & 0x00FF));
			try {
				bao.write((byte[]) o);
			} catch (IOException ex) {
				new BertException(ex.getMessage());
			}
		} else if (o instanceof Time) {
			Time time = (Time) o;

			Tuple tuple = new Tuple();
			tuple.add(new Atom("bert"));
			tuple.add(new Atom("time"));
			tuple.add(time.megasecond);
			tuple.add(time.second);
			tuple.add(time.microsecond);

			writeTuple(tuple);
		} else if (o instanceof Tuple) {
			writeTuple((Tuple) o);
		}

	}

	public byte[] encode(Object o) throws BertException {
		bao = new ByteArrayOutputStream();
		bao.write(-125);

		encodeTerm(o);

		return bao.toByteArray();
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

					Time time = new Time();

					time.timestamp = ((int) t.get(2) * (long) 1000000 * (long) 1000) + ((int) t.get(3) * (long) 1000) + ((int) t.get(4) / 1000);
					time.megasecond  = (int) t.get(2);
					time.second      = (int) t.get(3);
					time.microsecond = (int) t.get(4);

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
			Atom atom = new Atom(new String(val));
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
			try {
				return new String(val, "UTF-8");
			} catch (UnsupportedEncodingException ex) {
				return new String(val);
			}
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
