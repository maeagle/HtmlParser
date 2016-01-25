package com.maeagle.parser;

import org.junit.Test;

public class BookOrderParserTest {

    @Test
    public void testMain() {
        try {
            BookOrderParser.main(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
