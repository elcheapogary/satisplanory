/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.ui.jfx.persist;

import org.json.JSONObject;

public class PersistentData
{
    private String satisfactoryPath;

    static PersistentData fromJson(JSONObject json)
    {
        PersistentData retv = new PersistentData();

        retv.satisfactoryPath = json.optString("satisfactoryPath");

        return retv;
    }

    public String getSatisfactoryPath()
    {
        return satisfactoryPath;
    }

    public void setSatisfactoryPath(String satisfactoryPath)
    {
        this.satisfactoryPath = satisfactoryPath;
    }

    JSONObject toJson()
    {
        JSONObject json = new JSONObject();

        json.put("satisfactoryPath", satisfactoryPath);

        return json;
    }
}
