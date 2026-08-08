package slaynash.lum.bot.utils;

import java.awt.image.BufferedImage;

public final class ImageUtils {

    private static final double SSIM_C1 = 6.5025;
    private static final double SSIM_C2 = 58.5225;
    private static final int SSIM_WINDOW_SIZE = 8;

    public static double getImageSSIM(BufferedImage img0, BufferedImage img1) {
        float img0ratio = (float) img0.getWidth() / img0.getHeight();
        float img1ratio = (float) img1.getWidth() / img1.getHeight();

        if (Math.abs(img0ratio - img1ratio) > 0.1)
            return -1;

        // Ensure both images are the same size
        if (img0.getWidth() != img1.getWidth() || img0.getHeight() != img1.getHeight())
            img1 = resizeImage(img1, img0.getWidth(), img0.getHeight());

        // Convert images to grayscale
        byte[] grayImg0 = imageToGrayscale(img0);
        byte[] grayImg1 = imageToGrayscale(img1);

        return ssimGrayscaleImages(grayImg0, grayImg1, img0.getWidth(), img0.getHeight());
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        return resizedImage;
    }

    public static byte[] imageToGrayscale(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] gray = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[y * width + x] = (byte) ((r + g + b) / 3);
            }
        }
        return gray;
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
        double sum1 = 0, sum2 = 0;

        for (int j = 0; j < SSIM_WINDOW_SIZE; j++) {
            int y = startY + j;
            if (y >= height) break;
            for (int i = 0; i < SSIM_WINDOW_SIZE; i++) {
                int x = startX + i;
                if (x >= width) break;
                int idx = y * width + x;
                sum1 += img1[idx] & 0xFF;
                sum2 += img2[idx] & 0xFF;
                n++;
            }
        }

        if (n == 0) return 0.0;

        double mean1 = sum1 / n;
        double mean2 = sum2 / n;

        double varSum1 = 0, varSum2 = 0, covarSum = 0;
        for (int j = 0; j < SSIM_WINDOW_SIZE; j++) {
            int y = startY + j;
            if (y >= height) break;
            for (int i = 0; i < SSIM_WINDOW_SIZE; i++) {
                int x = startX + i;
                if (x >= width) break;
                int idx = y * width + x;
                double v1 = (img1[idx] & 0xFF) - mean1;
                double v2 = (img2[idx] & 0xFF) - mean2;
                varSum1 += v1 * v1;
                varSum2 += v2 * v2;
                covarSum += v1 * v2;
            }
        }

        double variance1 = varSum1 / (n - 1 > 0 ? n - 1 : 1);
        double variance2 = varSum2 / (n - 1 > 0 ? n - 1 : 1);
        double covariance = covarSum / (n - 1 > 0 ? n - 1 : 1);

        double numerator = (2 * mean1 * mean2 + SSIM_C1) * (2 * covariance + SSIM_C2);
        double denominator = (mean1 * mean1 + mean2 * mean2 + SSIM_C1) * (variance1 + variance2 + SSIM_C2);

        return numerator / denominator;
    }

}
