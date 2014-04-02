package org.bert;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.rules.ExpectedException;

@RunWith(JUnit4.class)
public class BertTest {

	@Test(expected = BertException.class)
    public void testInvalid() throws BertException {
		byte[] data = { 1, 2, 3 };
		Bert bert = new Bert(data);
    }

	@Test(expected = BertException.class)
    public void testAnotherInvalid() throws BertException {
		byte[] data = { (byte) 131, 8 };
		Bert bert = new Bert(data);
    }

}
