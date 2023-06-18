/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.gamedata.docload;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BracketObjectNotationTest
{
    @Test
    public void testArrayBasic()
            throws BracketObjectNotationParseException
    {
        List<String> l = BracketObjectNotation.parseArray("(a,b,c)");

        Assertions.assertEquals(3, l.size());
        Assertions.assertEquals("a", l.get(0));
        Assertions.assertEquals("b", l.get(1));
        Assertions.assertEquals("c", l.get(2));
    }

    @Test
    public void testQuotedStringsInArray()
            throws BracketObjectNotationParseException
    {
        List<String> l = BracketObjectNotation.parseArray("(\"a\",\"b\")");

        Assertions.assertEquals(2, l.size());
        Assertions.assertEquals("a", l.get(0));
        Assertions.assertEquals("b", l.get(1));
    }

    @Test
    public void testArrayNaive()
            throws BracketObjectNotationParseException
    {
        List<String> l = BracketObjectNotation.parseArray("((a=b,c=d),(e=f,g=h))");

        Assertions.assertEquals(2, l.size());
        Assertions.assertEquals("(a=b,c=d)", l.get(0));
        Assertions.assertEquals("(e=f,g=h)", l.get(1));
    }

    @Test
    public void testInvalidArrays()
    {
        Assertions.assertThrows(BracketObjectNotationParseException.class, () -> BracketObjectNotation.parseArray("a,b,c"));
        Assertions.assertThrows(BracketObjectNotationParseException.class, () -> BracketObjectNotation.parseArray(""));
        Assertions.assertThrows(BracketObjectNotationParseException.class, () -> BracketObjectNotation.parseArray("("));
        Assertions.assertThrows(BracketObjectNotationParseException.class, () -> BracketObjectNotation.parseArray("(()"));
    }

    @Test
    public void testObjectSimple()
            throws BracketObjectNotationParseException
    {
        BONObject o = BracketObjectNotation.parseObject("(a=b)");

        Assertions.assertEquals(1, o.size());
        Assertions.assertEquals("b", o.getString("a"));

        o = BracketObjectNotation.parseObject("(a=b,c=d)");

        Assertions.assertEquals(2, o.size());
        Assertions.assertEquals("b", o.getString("a"));
        Assertions.assertEquals("d", o.getString("c"));
    }
}
