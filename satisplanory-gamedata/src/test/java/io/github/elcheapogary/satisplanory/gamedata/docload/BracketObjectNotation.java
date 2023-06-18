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

import io.github.elcheapogary.satisplanory.util.function.throwing.ThrowingFunction;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * In Satisfactory {@code Docs.json} are these values in this object notation, like: {@code (key=value,key=value)} and
 * {@code (value,value,value)} etc. I don't know what the proper name is, so I'm calling it BracketObjectNotation.
 */
class BracketObjectNotation
{
    private BracketObjectNotation()
    {
    }

    public static List<String> parseArray(String s)
            throws BracketObjectNotationParseException
    {
        return parseArray(s.toCharArray(), ThrowingFunction.identity());
    }

    private static <T> List<T> parseArray(char[] chars, ThrowingFunction<String, T, BracketObjectNotationParseException> elementParser)
            throws BracketObjectNotationParseException
    {
        if (chars.length < 2 || chars[0] != '('){
            throw new BracketObjectNotationParseException("Invalid array: " + new String(chars));
        }

        List<T> retv = new LinkedList<>();
        int elementStart = 1;
        int nestedBracketLevel = 0;
        int i = 1;
        while (true){
            if (i == chars.length){
                throw new BracketObjectNotationParseException("Invalid array: " + new String(chars));
            }
            if (chars[i] == '('){
                nestedBracketLevel++;
            }else if (chars[i] == ',' && nestedBracketLevel == 0){
                String part = new String(chars, elementStart, i - elementStart);
                if (part.startsWith("\"") && part.endsWith("\"")){
                    part = part.substring(1, part.length() - 1);
                }
                retv.add(elementParser.apply(part));
                elementStart = i + 1;
            }else if (chars[i] == ')'){
                if (nestedBracketLevel == 0){
                    String part = new String(chars, elementStart, i - elementStart);
                    if (part.startsWith("\"") && part.endsWith("\"")){
                        part = part.substring(1, part.length() - 1);
                    }
                    retv.add(elementParser.apply(part));
                    if (i < chars.length - 1){
                        throw new BracketObjectNotationParseException("Invalid array: " + new String(chars));
                    }
                    break;
                }else{
                    nestedBracketLevel--;
                }
            }

            i++;
        }

        return retv;
    }

    public static <T> List<T> parseArray(String s, ThrowingFunction<String, T, BracketObjectNotationParseException> elementParser)
            throws BracketObjectNotationParseException
    {
        return parseArray(s.toCharArray(), elementParser);
    }

    public static BONObject parseObject(String s)
            throws BracketObjectNotationParseException
    {
        return parseObject(s.toCharArray());
    }

    private static BONObject parseObject(char[] chars)
            throws BracketObjectNotationParseException
    {
        if (chars.length < 2 || chars[0] != '('){
            throw new BracketObjectNotationParseException("Invalid object: " + new String(chars));
        }

        Map<String, String> map = new TreeMap<>();

        int i = 1;
        while (true){
            String key;
            {
                int keyStart = i;
                while (true){
                    if (i == chars.length){
                        throw new BracketObjectNotationParseException("Invalid object: " + new String(chars));
                    }
                    if (chars[i] == '='){
                        break;
                    }else if (chars[i] == ',' || chars[i] == ')' || chars[i] == '('){
                        throw new BracketObjectNotationParseException("Invalid object: " + new String(chars));
                    }
                    i++;
                }
                int keyEnd = i;
                i++;
                key = new String(chars, keyStart, keyEnd - keyStart);
            }
            int valueStart = i;
            int nestedBracketLevel = 0;
            while (true){
                if (i == chars.length){
                    throw new BracketObjectNotationParseException("Invalid object: " + new String(chars));
                }
                if ((chars[i] == ',' || chars[i] == ')') && nestedBracketLevel == 0){
                    break;
                }else if (chars[i] == '('){
                    nestedBracketLevel++;
                }else if (chars[i] == ')' && nestedBracketLevel > 0){
                    nestedBracketLevel--;
                }
                i++;
            }
            int valueEnd = i;
            String value = new String(chars, valueStart, valueEnd - valueStart);
            map.put(key, value);
            if (chars[i] == ')'){
                if (i < chars.length - 1){
                    throw new BracketObjectNotationParseException("Invalid object: " + new String(chars));
                }
                return new BONObject(map);
            }
            i++;
        }
    }

}
