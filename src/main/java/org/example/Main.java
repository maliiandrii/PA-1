package org.example;

import java.io.*;
import java.util.*;

public class Main {

    private static final int CHUNK_SIZE = 50 * 1024 * 1024;
    private static final int AVERAGE_NUMBER_SIZE = 11;

    public static void main(String[] args) throws IOException {
        String inputFile = "unsorted_numbers.txt";
        String outputFile = "sorted_numbers.txt";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the file size: ");
        int fileSizeInMB = scanner.nextInt();
        long numNumbers = calculateNumberCount(fileSizeInMB);

        System.out.println("Generating random numbers and writing them into a file...");
        generateRandomNumbers(inputFile, numNumbers);

        long startSort = System.currentTimeMillis();

        System.out.println("Splitting the file...");
        List<String> chunkFiles = splitFile(inputFile);

        System.out.println("Sorting the file parts...");

        for (String chunkFile : chunkFiles) {
            sortChunk(chunkFile);
        }

        System.out.println("Merging the file parts...");
        mergeSortedFiles(chunkFiles, outputFile);

        long endSort = System.currentTimeMillis();
        System.out.println("Sorting time: " + (endSort - startSort) / 1000 + " sec.\n");
    }

    private static long calculateNumberCount(int fileSizeInMB) {
        long fileSizeInBytes = fileSizeInMB * 1024L * 1024L;
        return fileSizeInBytes / AVERAGE_NUMBER_SIZE;
    }

    private static void generateRandomNumbers(String filename, long numNumbers) throws IOException {
        Random random = new Random();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (long i = 0; i < numNumbers; i++) {
                writer.write(random.nextInt(Integer.MAX_VALUE) + "\n");
            }
        }
    }

    private static List<String> splitFile(String inputFile) throws IOException {
        List<String> chunkFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            int counter = 0;
            int[] numbers = new int[CHUNK_SIZE / Integer.BYTES];
            int index = 0;

            while ((line = reader.readLine()) != null) {
                numbers[index++] = Integer.parseInt(line);

                if (index * Integer.BYTES >= CHUNK_SIZE) {
                    String chunkFile = "chunk_" + counter++ + ".txt";
                    writeChunkToFile(numbers, index, chunkFile);
                    chunkFiles.add(chunkFile);
                    index = 0;
                }
            }

            if (index > 0) {
                String chunkFile = "chunk_" + counter + ".txt";
                writeChunkToFile(numbers, index, chunkFile);
                chunkFiles.add(chunkFile);
            }
        }
        return chunkFiles;
    }

    private static void writeChunkToFile(int[] numbers, int length, String chunkFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chunkFile))) {
            for (int i = 0; i < length; i++) {
                writer.write(numbers[i] + "\n");
            }
        }
    }

    private static void sortChunk(String chunkFile) throws IOException {
        final int MAX_NUMBERS_IN_MEMORY = 1000000;
        List<Integer> numbers = new ArrayList<>(MAX_NUMBERS_IN_MEMORY);

        File tempFile = new File(chunkFile + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(chunkFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                numbers.add(Integer.parseInt(line));

                if (numbers.size() >= MAX_NUMBERS_IN_MEMORY) {
                    Collections.sort(numbers);
                    for (int num : numbers) {
                        writer.write(num + "\n");
                    }
                    numbers.clear();
                }
            }

            if (!numbers.isEmpty()) {
                Collections.sort(numbers);
                for (int num : numbers) {
                    writer.write(num + "\n");
                }
            }
        }

        File originalFile = new File(chunkFile);

        if (originalFile.delete()) {
            if (!tempFile.renameTo(originalFile)) {
                throw new IOException("Could not rename temp file to original chunk file.");
            }
        } else {
            throw new IOException("Could not delete original chunk file.");
        }
    }



    private static void mergeSortedFiles(List<String> chunkFiles, String outputFile) throws IOException {
        PriorityQueue<FileEntry> queue = new PriorityQueue<>(Comparator.comparingInt(e -> e.value));
        List<BufferedReader> readers = new ArrayList<>();


        try {
            for (String chunkFile : chunkFiles) {
                BufferedReader reader = new BufferedReader(new FileReader(chunkFile));
                readers.add(reader);
                String line = reader.readLine();
                if (line != null) {
                    queue.add(new FileEntry(Integer.parseInt(line), reader));
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                while (!queue.isEmpty()) {
                    FileEntry smallest = queue.poll();
                    writer.write(smallest.value + "\n");
                    String nextLine = smallest.reader.readLine();
                    if (nextLine != null) {
                        queue.add(new FileEntry(Integer.parseInt(nextLine), smallest.reader));
                    }
                }
            }
        } finally {
            for (BufferedReader reader : readers) {
                reader.close();
            }
        }
    }

    private static class FileEntry {
        int value;
        BufferedReader reader;

        FileEntry(int value, BufferedReader reader) {
            this.value = value;
            this.reader = reader;
        }
    }
}
