package com.kasabi.labs.datasets;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class TestValidateData {

	@Test
	public void validate() {
		File path = new File (Constants.DATA_PATH);
		assertTrue (Utils.validate(path));
	}

}
