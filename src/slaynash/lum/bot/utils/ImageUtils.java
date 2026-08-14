package slaynash.lum.bot.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public final class ImageUtils {

    private static final double SSIM_C1 = 6.5025;
    private static final double SSIM_C2 = 58.5225;
    private static final int SSIM_WINDOW_SIZE = 11;
    private static final int SSIM_STRIDE = 4;

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

    public static byte[] getSSIMData(BufferedImage img) {
        // Convert image to grayscale
        byte[] grayImg = imageToGrayscale(img);
        return grayImg;
    }

    public static double ssimCompare(byte[] grayImg1, int width1, int height1, BufferedImage img2) {
        // Ensure both images are the same size
        if (width1 != img2.getWidth() || height1 != img2.getHeight())
            img2 = resizeImage(img2, width1, height1);

        // Convert second image to grayscale
        byte[] grayImg2 = imageToGrayscale(img2);

        return ssimGrayscaleImages(grayImg1, grayImg2, width1, height1);
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
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
        for (int y = 0; y <= height - SSIM_WINDOW_SIZE; y += SSIM_STRIDE) {
            for (int x = 0; x <= width - SSIM_WINDOW_SIZE; x += SSIM_STRIDE) {
                ssimSum += computeSSIMWindow(img1, img2, width, height, x, y);
                windowCount++;
            }
        }

        // Note: We don't compare the last few pixels on the edges but it doesn't really matter

        return ssimSum / windowCount;
    }

    private static double computeSSIMWindow(byte[] img1, byte[] img2, int width, int height, int startX, int startY) {

        int windowWidth = Math.min(SSIM_WINDOW_SIZE, width - startX);
        int windowHeight = Math.min(SSIM_WINDOW_SIZE, height - startY);
        int n = windowWidth * windowHeight;
        if (n == 0)
            return 0.0;

        double sum1 = 0, sum2 = 0;
        for (int j = 0; j < windowHeight; j++) {
            int y = startY + j;
            for (int i = 0; i < windowWidth; i++) {
                int x = startX + i;
                int idx = y * width + x;
                sum1 += img1[idx] & 0xFF;
                sum2 += img2[idx] & 0xFF;
            }
        }

        double mean1 = sum1 / n;
        double mean2 = sum2 / n;

        double varSum1 = 0, varSum2 = 0, covarSum = 0;
        for (int j = 0; j < windowHeight; j++) {
            int y = startY + j;
            for (int i = 0; i < windowWidth; i++) {
                int x = startX + i;
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

    public static double getImageRatioDiff(BufferedImage img0, BufferedImage img1) {
        float img0ratio = (float) img0.getWidth() / img0.getHeight();
        float img1ratio = (float) img1.getWidth() / img1.getHeight();

        return Math.abs(img0ratio - img1ratio);
    }

}
