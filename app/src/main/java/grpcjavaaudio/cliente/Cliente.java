package grpcjavaaudio.cliente;

import com.proto.audio.Audio.DownloadFileRequest;
import com.proto.audio.AudioServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.*;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

//Cristopher Vázquez Villa

public class Cliente {
    public static void main(String[] args) {
        String host = "localhost";
        int puerto = 8080;
        String nombre;

        ManagedChannel ch = ManagedChannelBuilder.forAddress(host, puerto).usePlaintext().build();

        nombre = "tiesto.mp3";
        ByteArrayInputStream streamMp3 = downloadFile(ch, nombre);
        playMp3(streamMp3, nombre);
        try {
            streamMp3.close();
        } catch (IOException e) {

        }
        //streamWav(ch, nombre, 44100f);

        System.out.println("Apagando...");
        ch.shutdown();
    }

    public static void streamWav(ManagedChannel ch, String nombre, float sampleRate) {
        try {
            AudioFormat newFormat = new AudioFormat(sampleRate, 16, 2, true, false);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(newFormat);
            sourceDataLine.open(newFormat);
            sourceDataLine.start();

            AudioServiceGrpc.AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);
            DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

            int bufferSize = 1024;
            System.out.println("Reproduciendo el archivo: " + nombre);

            stub.downloadAudio(peticion).forEachRemaining(respuesta -> {
                try {
                    sourceDataLine.write(respuesta.getData().toByteArray(), 0, bufferSize);
                    System.out.print(".");
                } catch (Exception e) {
                }
            });
            System.out.println("\nRecepción de datos correcta.");
            System.out.println("Reproduccion terminada.\n\n");

            sourceDataLine.drain();
            sourceDataLine.close();
        } catch (LineUnavailableException e) {
            System.err.print(e.getMessage());
        }
    }
    
    public static ByteArrayInputStream downloadFile(ManagedChannel ch, String nombre) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        AudioServiceGrpc.AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);
        DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

        System.out.println("Recibiendo el archivo: " + nombre);
        stub.downloadAudio(peticion).forEachRemaining(respuesta -> {
            try {
                stream.write(respuesta.getData().toByteArray());
                System.out.print(".");
            } catch (IOException e) {
                System.out.println("No se pudo obtener el archivo de audio." + e.getMessage());
            }
        });

        System.out.println("\nRecepción de datos correcta.\n\n");

        return new ByteArrayInputStream(stream.toByteArray());
    }

    public static void playWav(ByteArrayInputStream inStream, String nombre) {
        try {
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(inStream);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n\n");
            clip.start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            }
            clip.stop();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void playMp3(ByteArrayInputStream inStream, String nombre) {
        try {
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n\n");
            Player player = new Player(inStream);
            player.play();
        } catch (JavaLayerException e) {

        }
    }
}
