package ch.so.agi.avdpool.camel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.interlis2.av2geobau.Av2geobau;

import ch.ehi.basics.settings.Settings;

public class Av2GeobauProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        File itfFile = exchange.getIn().getBody(File.class);
        Path tempDir = Files.createTempDirectory("av2geobau_");
        File dxfFile = Paths.get(tempDir.toFile().getAbsolutePath(), itfFile.getName().replaceFirst("[.][^.]+$", "") + ".dxf").toFile();        

        Settings settings=new Settings();
        settings.setValue(Av2geobau.SETTING_ILIDIRS, Av2geobau.SETTING_DEFAULT_ILIDIRS);
        
        try {
            boolean ok = Av2geobau.convert(itfFile, dxfFile, settings);
            if (!ok) {
                throw new Exception("could not convert: " + itfFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("could not convert: " + itfFile.getAbsolutePath());
        }
        
        File layerDefinitionFile = Paths.get(tempDir.toFile().getAbsolutePath(), "DXF_Geobau_Layerdefinition.pdf").toFile();
        InputStream layerDefinitionInputStream = Av2GeobauProcessor.class.getResourceAsStream("/DXF_Geobau_Layerdefinition.pdf"); 
        Files.copy(layerDefinitionInputStream, layerDefinitionFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        layerDefinitionInputStream.close();
        
        File hintsFile = Paths.get(tempDir.toFile().getAbsolutePath(), "Hinweise.pdf").toFile();
        InputStream hintsInputStream = Av2GeobauProcessor.class.getResourceAsStream("/Hinweise.pdf"); 
        Files.copy(hintsInputStream, hintsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        hintsInputStream.close();
        
        File sampleFile = Paths.get(tempDir.toFile().getAbsolutePath(), "Musterplan.pdf").toFile();
        InputStream sampleInputStream = Av2GeobauProcessor.class.getResourceAsStream("/Musterplan.pdf"); 
        Files.copy(sampleInputStream, sampleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        sampleInputStream.close();

        String outZipFileName = Paths.get(tempDir.toFile().getAbsolutePath(), itfFile.getName().replaceFirst("[.][^.]+$", "") + ".zip").toFile().getAbsolutePath();
        FileOutputStream fileOutputStream = new FileOutputStream(outZipFileName);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        // add dxf geobau layer definition pdf to zip file
        ZipEntry layerDefinitionZipEntry = new ZipEntry(layerDefinitionFile.getName());
        zipOutputStream.putNextEntry(layerDefinitionZipEntry);
        new FileInputStream(layerDefinitionFile).getChannel().transferTo(0, layerDefinitionFile.length(), Channels.newChannel(zipOutputStream));

        // add hints pdf to zip file
        ZipEntry hintsZipEntry = new ZipEntry(hintsFile.getName());
        zipOutputStream.putNextEntry(hintsZipEntry);
        new FileInputStream(hintsFile).getChannel().transferTo(0, hintsFile.length(), Channels.newChannel(zipOutputStream));

        // add sample pdf to zip file
        ZipEntry sampleZipEntry = new ZipEntry(sampleFile.getName());
        zipOutputStream.putNextEntry(sampleZipEntry);
        new FileInputStream(sampleFile).getChannel().transferTo(0, sampleFile.length(), Channels.newChannel(zipOutputStream));

        // add dxf geobau file to zip file
        ZipEntry dxfZipEntry = new ZipEntry(dxfFile.getName());
        zipOutputStream.putNextEntry(dxfZipEntry);
        new FileInputStream(dxfFile).getChannel().transferTo(0, dxfFile.length(), Channels.newChannel(zipOutputStream));

        zipOutputStream.closeEntry();
        zipOutputStream.close();

        exchange.getIn().setBody(new File(outZipFileName));
    }
}
