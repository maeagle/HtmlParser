package com.maeagle.parser;

import com.maeagle.parser.business.BookOrderThread;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

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
