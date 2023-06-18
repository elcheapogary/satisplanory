package io.github.elcheapogary.satisplanory.gamedata.docload;

import io.github.elcheapogary.satisplanory.gamedata.GameData;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

public final class DocsToSatisplanoryJson
{
    private DocsToSatisplanoryJson()
    {
    }

    public static void main(String[] args)
            throws IOException, DataException
    {
        File inputFile = new File("u7-docs.json");
        File outputFile = new File("u7-data.json");

        GameData gd;

        {
            GameData.Builder builder = new GameData.Builder();
            DocsJsonLoader.loadDocsJson(builder, inputFile);
            gd = builder.build();
        }

        try (JsonWriter w = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(outputFile.toPath()), StandardCharsets.UTF_8), 4096 * 4))){
            w.writeObject(gd.toJson());
        }
    }
}
