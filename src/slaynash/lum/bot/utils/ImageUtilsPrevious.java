package slaynash.lum.bot.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ImageUtilsPrevious {

    private static final double SSIM_C1 = 6.5025;
    private static final double SSIM_C2 = 58.5225;
    private static final int SSIM_WINDOW_SIZE = 8;

    public static byte[] getSSIMData(BufferedImage img) {
        // Convert image to grayscale
        return imageToGrayscale(img);
    }

    public static double ssimCompare(byte[] grayImg1, int width1, int height1, BufferedImage img2) {
        return prepareComparison(img2).ssimCompare(grayImg1, width1, height1);
    }

    public static PreparedComparison prepareComparison(BufferedImage image) {
        return new PreparedComparison(image);
    }

    public static final class PreparedComparison {
        private final BufferedImage image;
        private final ConcurrentMap<Long, byte[]> grayscaleImages = new ConcurrentHashMap<>();

        private PreparedComparison(BufferedImage image) {
            this.image = image;
        }

        public double ssimCompare(byte[] grayImg1, int width1, int height1) {
            float img1Ratio = (float) width1 / height1;
            float img2Ratio = (float) image.getWidth() / image.getHeight();

            if (Math.abs(img1Ratio - img2Ratio) > 0.1)
                return -1;

            long sizeKey = ((long) width1 << 32) | (height1 & 0xFFFFFFFFL);
            byte[] grayImg2 = grayscaleImages.computeIfAbsent(sizeKey, key -> {
                BufferedImage resizedImage = width1 == image.getWidth() && height1 == image.getHeight()
                    ? image
                    : resizeImage(image, width1, height1);
                return imageToGrayscale(resizedImage);
            });

            return ssimGrayscaleImages(grayImg1, grayImg2, width1, height1);
        }
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight,
            originalImage.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : originalImage.getType());
        Graphics graphics = resizedImage.getGraphics();
        try {
            graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        }
        finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    public static byte[] imageToGrayscale(BufferedImage img) {
        BufferedImage grayscale = new BufferedImage(
            img.getWidth(),
            img.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );

        // Draw the original image onto the grayscale canvas
        Graphics g = grayscale.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return ((DataBufferByte) grayscale.getRaster().getDataBuffer()).getData();
    }

    public static double ssimGrayscaleImages(byte[] img1, byte[] img2, int width, int height) {
        double ssimSum = 0.0;
        int windowCount = 0;
        for (int y = 0; y <= height - SSIM_WINDOW_SIZE; y += SSIM_WINDOW_SIZE) {
            for (int x = 0; x <= width - SSIM_WINDOW_SIZE; x += SSIM_WINDOW_SIZE) {
                ssimSum += computeSSIMWindow(img1, img2, width, height, x, y);
                windowCount++;
            }
        }

        // Note: We don't compare the last few pixels on the edges but it doesn't really matter

        return ssimSum / windowCount;
    }

    private static double computeSSIMWindow(byte[] img1, byte[] img2, int width, int height, int startX, int startY) {
        int n = 0;
        double sum1 = 0, sum2 = 0, squareSum1 = 0, squareSum2 = 0, productSum = 0;

        for (int j = 0; j < SSIM_WINDOW_SIZE; j++) {
            int y = startY + j;
            if (y >= height) break;
            for (int i = 0; i < SSIM_WINDOW_SIZE; i++) {
                int x = startX + i;
                if (x >= width) break;
                int idx = y * width + x;
                int value1 = img1[idx] & 0xFF;
                int value2 = img2[idx] & 0xFF;
                sum1 += value1;
                sum2 += value2;
                squareSum1 += value1 * value1;
                squareSum2 += value2 * value2;
                productSum += value1 * value2;
                n++;
            }
        }

        if (n == 0) return 0.0;

        double mean1 = sum1 / n;
        double mean2 = sum2 / n;
        double divisor = n - 1 > 0 ? n - 1 : 1;
        double variance1 = (squareSum1 - sum1 * mean1) / divisor;
        double variance2 = (squareSum2 - sum2 * mean2) / divisor;
        double covariance = (productSum - sum1 * mean2) / divisor;

        double numerator = (2 * mean1 * mean2 + SSIM_C1) * (2 * covariance + SSIM_C2);
        double denominator = (mean1 * mean1 + mean2 * mean2 + SSIM_C1) * (variance1 + variance2 + SSIM_C2);

        return numerator / denominator;
    }

}
