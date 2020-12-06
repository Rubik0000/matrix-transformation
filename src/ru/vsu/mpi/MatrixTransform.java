package ru.vsu.mpi;

import mpi.Datatype;
import mpi.MPI;

import java.util.Arrays;
import java.util.Random;

/**
 * Создать описатель типа и использовать его при передаче данных в качестве шаблона для преобразования
 * Номер 6
 *
 * Запуск: mpjrun.sh -np 1 ru.vsu.mpi.MatrixTransform <rows> <columns>
 */
public class MatrixTransform {

    public static final int TAG = 0;
    public static final String POSITIVE_INTEGER_NUMBER_REGEXP = "^\\d+$";


    public static void main(String[] args) {
        try {
            MPI.Init(args);
            validateCla(args);
            int rank = MPI.COMM_WORLD.Rank();
            int rows = Integer.parseInt(args[3]);
            int cols = Integer.parseInt(args[4]);
            int[][] sendBuffMatr = randMatrix(rows, cols);
            printMatrix(sendBuffMatr);
            System.out.println(getDelimiter(cols));

            Datatype type = Datatype.Indexed(createBlockSizes(rows, cols), createBlockOffsets(rows, cols), MPI.INT);
            int[] recvBuff = new int[rows * cols];
            MPI.COMM_WORLD.Sendrecv(
                    flatMatrix(sendBuffMatr), 0, 1, type, rank, TAG,
                    recvBuff, 0, recvBuff.length, MPI.INT, rank, TAG
            );
            printMatrix(formatTransformation(recvBuff, rows, cols));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } finally {
            MPI.Finalize();
        }
    }

    private static void validateCla(String[] args) {
        if (args.length < 5) {
            throw new IllegalArgumentException("Expected 2 argument <rows> <columns>");
        }
        if (!args[3].matches(POSITIVE_INTEGER_NUMBER_REGEXP)) {
            throw new IllegalArgumentException("Invalid number of rows");
        }
        if (!args[4].matches(POSITIVE_INTEGER_NUMBER_REGEXP)) {
            throw new IllegalArgumentException("Invalid number of columns");
        }
    }

    private static int[][] randMatrix(int rows, int cols) {
        Random random = new Random();
        int[][] matr = new int[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                matr[i][j] = random.nextInt(90) + 10;
            }
        }
        return matr;
    }

    private static void printMatrix(int[][] matr) {
        for (int[] row : matr) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static void printMatrix(String[][] matr) {
        for (String[] row : matr) {
            System.out.println(Arrays.toString(row));
        }
    }

    private static int[] flatMatrix(int[][] matr) {
        int len = matr.length * matr[0].length;
        int[] array = new int[len];
        for (int i = 0; i < matr.length; ++i) {
            System.arraycopy(matr[i], 0, array, i * matr[i].length, matr[i].length);
        }
        return array;
    }

    private static int[][] unflatMatrix(int[] array, int rows, int cols) {
        int[][] matr = new int[rows][cols];
        int rowCount = 0;
        int colCount = 0;
        for (int value : array) {
            matr[rowCount][colCount] = value;
            if (colCount == cols - 1) {
                ++rowCount;
                colCount = 0;
            } else {
                ++colCount;
            }
        }
        return matr;
    }

    private static int[] createBlockSizes(int rows, int cols) {
        int min = Math.min(rows, cols);
        int[] blockSizes = new int[min * 2 - 2];
        Arrays.fill(blockSizes, 1);
        blockSizes[0] = cols;
        blockSizes[blockSizes.length - 1] = Math.max(1, cols - rows + 1);
        return blockSizes;
    }


    private static int[] createBlockOffsets(int rows, int cols) {
        int min = Math.min(rows, cols);
        int[] blockOffset = new int[min * 2 - 2];
        blockOffset[0] = 0;
        int offset = cols + 1;
        int j = 1;
        for (int i = 1; i < min - 1; ++i) {
            blockOffset[j] = offset;
            blockOffset[j + 1] = cols * (i + 1) - 1;
            offset += cols + 1;
            j += 2;
        }
        blockOffset[blockOffset.length - 1] = cols < rows ? cols * cols - 1 : rows * cols - Math.max(0, cols - rows) - 1;
        return blockOffset;
    }

    private static String[][] formatTransformation(int[] result, int rows, int cols) {
        String[][] formatted = new String[rows][cols];
        for (int i = 0; i < cols; ++i) {
            formatted[0][i] = String.valueOf(result[i]);
        }
        for (int i = 1; i < rows; ++i) {
            Arrays.fill(formatted[i], "* ");
        }
        int offset = cols;
        int min = Math.min(rows, cols);
        for (int i = 1; i < min; ++i) {
            if (i == min - 1) {
                int j = i;
                for (int k = 0; k < Math.max(cols - rows, 0) + 1; ++k) {
                    formatted[i][j++] = String.valueOf(result[offset++]);
                }
            } else {
                formatted[i][i] = String.valueOf(result[offset]);
                formatted[i][cols - 1] = String.valueOf(result[offset + 1]);
                offset += 2;
            }
        }
        return formatted;
    }

    private static String getDelimiter(int cols) {
        return "----".repeat(Math.max(0, cols));
    }

    // транспонирование матрицы
    //type = Datatype.Vector(rows, cols, cols, MPI.INT);
}
