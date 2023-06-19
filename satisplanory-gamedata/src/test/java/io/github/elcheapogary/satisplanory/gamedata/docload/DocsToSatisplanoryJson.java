package io.github.elcheapogary.satisplanory.gamedata.docload;

import io.github.elcheapogary.satisplanory.gamedata.GameData;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
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
        File inputFile = new File("u8-docs.json");
        File outputFile = new File("u8-data.json");

        GameData gd;

        {
            GameData.Builder builder = new GameData.Builder();
            DocsJsonLoader.loadDocsJson(builder, inputFile);

            Map<String, Long> resources = new TreeMap<>();

            resources.put("Iron Ore", 70380L);
            resources.put("Copper Ore", 28860L);
            resources.put("Limestone", 52860L);
            resources.put("Coal", 30900L);
            resources.put("Caterium Ore", 11040L);
            resources.put("Raw Quartz", 10500L);
            resources.put("Sulfur", 6840L);
            resources.put("Uranium", 2100L);
            resources.put("Bauxite", 9780L);
            resources.put("Crude Oil", 11700L);
            resources.put("Nitrogen Gas", 12000L);

            for (var entry : resources.entrySet()){
                builder.addRawResources(builder.getItemByName(entry.getKey()).orElseThrow(() -> new DataException("Missing item: " + entry.getKey())), entry.getValue());
            }

            gd = builder.build();
        }

        try (JsonWriter w = Json.createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(outputFile.toPath()), StandardCharsets.UTF_8), 4096 * 4))){
            w.writeObject(gd.toJson());
        }
    }
}
