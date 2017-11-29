package com.maeagle.parser;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContext-core.xml"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClicElearnParserTest {

    @Test
    public void testMain() throws Exception {
        ClicElearnParser.main(null);
    }

}
