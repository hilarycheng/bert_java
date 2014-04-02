package org.bert;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

public class Bert {

	public enum Type {
		INT,
		FLOAT,
		ATOM,
		TUPLE,
		NIL,
		STRING,
		LIST,
		IMPROPER_LIST,
		BINARY
	};

	private class Atom {
		public String value;

		public String toString() {
			return value;
		}
	}

	private class Tuple {
		public Bert mValue;

		public String toString() {
			return mValue.toString();
		}
	}

	private class ImproperList_ {
		public Bert mValue;

		public String toString() {
			return mValue.toString();
		}
	}

	private class List_ {
		public Bert mValue;

		public String toString() {
			return mValue.toString();
		}
	}

	private ArrayList<Object> mValue = null;

	private int itemConut = 0;

	public Bert() {
	}

	public Bert(final byte[] data) throws BertException {
		if ((data[0] & 0x00FF) == 131) {
			mValue = new ArrayList<Object>();
			decode(data, 1, mValue);
		} else {
			throw new BertException("Invalid Bert Data");
		}
	}

	private int decodeSmallTuple(final byte[] data, int offset, ArrayList<Object> arr) throws BertException {
		int i = 0;
		int size = data[offset] & 0x00FF;
		int off  = offset + 1;

		Tuple t = new Tuple();
		t.mValue = new Bert();
		t.mValue.mValue = new ArrayList<Object>();

		for (i = 0; i < size; i++) {
			off = decodeOnce(data, off, t.mValue.mValue);
		}

		arr.add(t);

		return off;
	}

	private int decodeBinary(final byte[] data, int offset, ArrayList<Object> arr) {
		int i = 0;
		int off  = offset;
		int size = ((data[off] & 0x00FF) << 24) +
			((data[off + 1] & 0x00FF) << 16) +
			((data[off + 2] & 0x00FF) << 8) +
			(data[off + 3] & 0x00FF);
		off = offset + 4;

		byte[] d = new byte[size];
		System.arraycopy(data, offset + 4, d, 0, size);
		arr.add(d);

		off = off + size;

		return off;
	}

	private int decodeLargeTuple(final byte[] data, int offset, ArrayList<Object> arr) throws BertException {
		int i = 0;
		int off  = offset;
		int size = ((data[off] & 0x00FF) << 24) +
			((data[off + 1] & 0x00FF) << 16) +
			((data[off + 2] & 0x00FF) << 8) +
			(data[off + 3] & 0x00FF);
		off = offset + 4;

		Tuple t = new Tuple();
		t.mValue = new Bert();
		t.mValue.mValue = new ArrayList<Object>();

		for (i = 0; i < size; i++) {
			off = decodeOnce(data, off, t.mValue.mValue);
		}

		arr.add(t);

		return off;
	}

	private int decodeList(final byte[] data, int offset, ArrayList<Object> arr) throws BertException {
		int i = 0;
		int off  = offset;
		int size = ((data[off] & 0x00FF) << 24) +
			((data[off + 1] & 0x00FF) << 16) +
			((data[off + 2] & 0x00FF) << 8) +
			(data[off + 3] & 0x00FF);
		off = offset + 4;

		ArrayList<Object> temp = new ArrayList<Object>();

		for (i = 0; i < size + 1; i++) {
			off = decodeOnce(data, off, temp);
		}

		if (temp.get(size) == null) {
			temp.remove(size);
			List_ t = new List_();
			t.mValue = new Bert();
			t.mValue.mValue = temp;
			arr.add(t);
		} else {
			ImproperList_ t = new ImproperList_();
			t.mValue = new Bert();
			t.mValue.mValue = temp;
			arr.add(t);
		}


		return off;
	}

	private int decodeString(final byte[] data,  int offset, ArrayList<Object> arr) {
		int size = ((data[offset] & 0x00FF) << 8) + (data[offset + 1] & 0x00FF);
		String s = new String(data, offset + 2, size);

		arr.add(s);

		return offset + size + 2;
	}

	private int decodeAtom(final byte[] data,  int offset, ArrayList<Object> arr) {
		int size = ((data[offset] & 0x00FF) << 8) + (data[offset + 1] & 0x00FF);
		String s = new String(data, offset + 2, size);

		Atom at = new Atom();
		at.value = s;
		arr.add(at);

		return offset + size + 2;
	}

	private int decodeOnce(final byte[] data, int offset, ArrayList<Object> arr) throws BertException {
		int off = offset;
		int i = 0;
		int size = 0;
		String s = null;
		ByteBuffer b = null;

		switch (data[off] & 0x00FF) {
		case 97:  // SmallInt Tag
			arr.add((data[off + 1] & 0x00FF));
			off += 2;
			break;
		case 98:  // Int Tag
			size = ((data[off + 1] & 0x00FF) << 24) +
				((data[off + 2] & 0x00FF) << 16) +
				((data[off + 3] & 0x00FF) << 8) +
				(data[off + 4] & 0x00FF);
			b = ByteBuffer.wrap(data, off + 1, 4);
			arr.add(b.getInt());
			off += 5;
			break;
		case 110: // SmallBignumTag
			// Log.i("Websocket", "Bert Small Big Num");
			break;
		case 111: // LargeBignumTag
			// Log.i("Websocket", "Bert Large Big Num");
			break;
		case 99:  // FloatTag
			s = new String(data, off + 1, 31);
			arr.add(Float.parseFloat(s));
			off += 32; 
			break;
		case 100: // AtomTag
			off = decodeAtom(data, off + 1, arr);
			break;
		case 104: // SmallTupleTag
			off = decodeSmallTuple(data, off + 1, arr);
			break;
		case 105: // LargeTupleTag
			off = decodeLargeTuple(data, off + 1, arr);
			break;
		case 106: // NilTag
			arr.add(null);
			off++;
			break;
		case 107: // StringTag
			off = decodeString(data, off + 1, arr);
			break;
		case 108: // ListTag
			off = decodeList(data, off + 1, arr);
			break;
		case 109: // BinTag
			off = decodeBinary(data, off + 1, arr);
			break;
		default:
			throw new BertException("Invalid Bert Data");
		}

		return off;
	}

	private int decode(final byte[] data, int offset, ArrayList<Object> arr) throws BertException {
		int off = offset;

		for (off = offset; off < data.length;) {
			off = decodeOnce(data, off, arr);
		}

		return off;
	}

	public void addInteger(int value) {
	}

	public void addFloat(float value) {
	}

	public void addAtom(String value) {
	}

	public void addNil() {
	}

	public void addString(String value) {
	}

	public void addTuple(List<Bert> bert)  {
	}

	public void addList(List<Bert> bert)  {
	}

	public int getItemCount() {
		return mValue == null ? 0 : mValue.size();
	}

	public Type getItemType(int count) {
		return Type.NIL;
	}

	public int getInt(int count) {
		return 0;
	}

	public float getFloat(int count) {
		return 0;
	}

	public String getAtom(int count) {
		return null;
	}

	public boolean isNil(int count) {
		return false;
	}

	public String getString(int count) {
		return null;
	}

	public byte[] getBinary(int count) {
		return null;
	}

	public Bert getTuples(int count) {
		return null;
	}

	public Bert getList(int count) {
		return null;
	}

	public String toString() {
		return mValue == null ? null : mValue.toString();
	}

}
