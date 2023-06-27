package com.mobichord.ftps.utility;

import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.mobichord.ftps.data.AttachmentDownloadResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Okio;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Slf4j
public class FtpsUtils {

    public static final String YYYY_M_MDD_H_HMMSS = "yyyyMMdd_HHmmss";

    public static final List<Character> META_CHARACTERS = List.of('.', '^', '$', '*', '+', '-', '?', '(', ')', '[', ']', '{', '}', '\\', '|');

    public static String[] filterByPattern(List<String> input, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern);

        return input
                .stream()
                .filter(compiledPattern.asPredicate())
                .toArray(String[]::new);
    }

    public static List<String> filterList(List<String> inputList, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern);

        return inputList.stream()
                .filter(compiledPattern.asPredicate())
                .collect(Collectors.toList());
    }

    public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }

    public static String getNewFileName(String originalFileNameWithExtension, String remoteFilePath) {
        log.debug("Converting original file name: {}", originalFileNameWithExtension);
        int dotIndex = originalFileNameWithExtension.lastIndexOf('.');
        String fileNameWithoutExtension = (dotIndex == -1) ? originalFileNameWithExtension : originalFileNameWithExtension.substring(0, dotIndex);
        String fileExtension = (dotIndex == -1) ? "" : originalFileNameWithExtension.substring(dotIndex + 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_M_MDD_H_HMMSS);
        String timestamp = dateFormat.format(new Date());
        String newFileName = FilenameUtils.concat(remoteFilePath, fileNameWithoutExtension +"-"+ timestamp + "." + fileExtension);

        log.debug("Converted file name: {}", newFileName);
        return newFileName;
    }

    public static String getFileNameWithPath(String originalFileNameWithExtension, String remoteFilePath) {
        log.debug("Converting original file name: {}", originalFileNameWithExtension);
        String newFileName = FilenameUtils.concat(remoteFilePath, originalFileNameWithExtension);

        log.debug("Converted file name: {}", newFileName);
        return newFileName;
    }

    public static AttachmentDownloadResponse getAttachmentById(String anyString) {

        File file = new File("src/main/resources/Paymentfile-.csv");

        // Create a dummy body
        okhttp3.RequestBody body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"), file);
        Response response;
        AttachmentDownloadResponse attachmentDownloadResponse = null;

        try {
            // create okhttp response object
            response = new Response.Builder()
                    .request(new okhttp3.Request.Builder().url("http://localhost").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Disposition", "attachment; filename=" + file.getName())
                    .body(new RealResponseBody("application/octet-stream", file.length(), Okio.buffer(Okio.source(file))))
                    .build();

            attachmentDownloadResponse = new AttachmentDownloadResponse(response);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return attachmentDownloadResponse;
    }

    public static Map<String, String> adjustAttachmentFileNames(List<String> files, String fileSeparator) {
        Map<String, String> result = new HashMap<>();

        Map<String, List<String>> fileMap = files.stream()
                .collect(Collectors.groupingBy(FtpsUtils::getName));

        for (var entry : fileMap.entrySet()) {
            final String name = entry.getKey();
            final List<String> paths = entry.getValue();
            if (paths.size() == 1) {
                result.put(paths.get(0), name);
                continue;
            }

            Deque<ImmutablePair<String[], String>> pairs = paths.stream()
                    .map(x -> {
                        var segments = getPathSegments(x, fileSeparator);
                        Collections.reverse(segments);
                        return new ImmutablePair<>(segments.toArray(new String[0]), x);
                    })
                    .collect(Collectors.toCollection(ArrayDeque::new));

            final int maxSegmentIndex = pairs.stream().map(x -> x.left.length)
                    .min(Comparator.naturalOrder())
                    .orElse(0);

            var groupName = name;
            for (int si = 1; si < maxSegmentIndex; si++) {
                final int segmentIndex = si;
                String segment = "";

                final var numberOfPairs = pairs.size();
                for (var pi = 0; pi < numberOfPairs; pi++) {
                    var item = pairs.pop();
                    final String localSegment = item.left[segmentIndex];
                    if (pairs.stream().noneMatch(x -> x.left[segmentIndex].equals(localSegment))) {
                        result.put(item.right, String.format("%s_%s", localSegment, groupName));
                    } else {
                        pairs.addLast(item);
                        segment = localSegment;
                    }
                }

                groupName = String.format("%s_%s", segment, groupName);
                if (pairs.size() == 1) {
                    result.put(pairs.getFirst().right, groupName);
                    break;
                } else if (pairs.size() == 0) {
                    break;
                }
            }
        }

        return result;
    }

    private static String getName(String input) {
        String path = input;
        try {
            var url = new URL(input);
            var protocol = url.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new RuntimeException(String.format("URL protocol %s is not supported.", protocol));
            }

            path = url.getPath();
        } catch (MalformedURLException ignore) {
        }

        return FilenameUtils.getName(path);
    }

    private static List<String> getPathSegments(String input, String fileSeparator) {
        try {
            var url = new URL(input);
            var protocol = url.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new RuntimeException(String.format("URL protocol %s is not supported.", protocol));
            }

            return Arrays.asList(url.getPath().split("/"));
        } catch (MalformedURLException ignore) {
        }

        // At this point FS path is expected
        return Arrays.stream(input.split(Pattern.quote(fileSeparator))).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    //use to get path of uploads
    public static String getFinalDirectoryPath(String path1, String path2) {
        if ((path1 == null || path1.isEmpty()) && (path2 == null || path2.isEmpty())) {
            return "/";
        }
        return (path2 != null && !path2.isEmpty()) ? path2 : path1;
    }

    public static CryptoConfiguration getTestCryptoConfiguration() {
        CryptoConfiguration cryptoConfiguration = new CryptoConfiguration();
        cryptoConfiguration.setPublicKey("-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
                "\n" +
                "mQENBF/PUJcBCACfadnXlu2VU2yQsZUjqxQQ+ql59U7J2AGK2Kg0wUmYoocapNA4\n" +
                "Q4ILgYUmYn601f2uANmTxLwHXUGdpAO+yOIRKA7Z9PjP5+Qad8TXnyZ+ZH70HFXi\n" +
                "EwCz8r5e2tGT0JmXVxD+hBKwYihCBB5V21L3+FJYGpyWcJ+hRbs5Dyq5oIrk+DQL\n" +
                "BYbVXSYcUnNyBFdquuIAD4klwqTXjXswhfSObsX82vp+poEiJZ5kNxsw2BBD3PcR\n" +
                "Ma3uYuEtezT2uLR0K/D1jDWdXlZgwiWg2W2Pj8WvxTwV3FcE8Bz539uPgUKtMkgv\n" +
                "a323Unw+/UUkVhjRhRz1xq+dCgd8/WFFj7fXABEBAAG0HUpvaG5Eb2UgPGpvaG5k\n" +
                "b2VAZXhhbXBsZS5jb20+iQFUBBMBCAA+FiEEqBNbGAb/ahfilignePbHMj+oaVEF\n" +
                "Al/PUJcCGwMFCQeGHuwFCwkIBwIGFQoJCAsCBBYCAwECHgECF4AACgkQePbHMj+o\n" +
                "aVEJsQgAi80wM0fJji+PqKBTxLf1Nyvq48mwBIowlBhKkafflPTEPwjZfvfPkVKA\n" +
                "kEKJfxisyzF/OVBlo0YIv2D+DPI40q4IBK9Z/2NoSYqE8mXGlanp5oFauJnQflnd\n" +
                "oB0TibJlJ3xDJtGtLKYa9mgI6kf/keg54OfcFgideno5LPlAck/j/rzE4jr///C1\n" +
                "sJhwhUjedKSJrBPjTTzNTUWC6me3wW9O8sTBBscRISKVo5vcr2rXq3lLEhi4fA3w\n" +
                "U8YbDyGE0LwAdmNeTFGPD8lFEocpzyFHSoVU0G2rsGhz3VTK/vJDuD5cyfkUeMYh\n" +
                "4yAmk6z2VH9uLC0SZgW+Z4TQ4g6HnLkBDQRfz1CXAQgAtHYoOH5yPgVBkFxJN/FJ\n" +
                "6zXU9Ei5yN9ZUxxK6eKH3TyJ5/pBSu8sHRTC1sK5cpe8ymRwEZ5jw0kTqQta6DDa\n" +
                "29Lq81e56RnDEZKQxguWxaxGrMqO9mypw89i6y5WXNgV9REm76Xr4ImIhAE+mKKv\n" +
                "Qz8KBhmgpDj7qqcNMPzX2llK0eJnph5xxmg+7h3E/+ckc0WKLGgRgaQdjNoxV7iL\n" +
                "SYeNh2/TWzTUN4ZCZ9r+4NbB+FgRS7/8Np9e/4IJDcO2ybigs7scbbLYORbsxp+E\n" +
                "Ind5SaP9RSqOcKHxHKwJhxQOa9I6DPlK3CDEobnJQ7/4H3yfsTY2+GreUdtKf7Zy\n" +
                "LQARAQABiQE8BBgBCAAmFiEEqBNbGAb/ahfilignePbHMj+oaVEFAl/PUJcCGwwF\n" +
                "CQeGHuwACgkQePbHMj+oaVEX6Qf/frRmVsChsoWP/hfzdPSoHfA27rkqoAaJkdaP\n" +
                "cN85dQgQ5Ak78JtpJ729cJUI6TIRLCE4M+Go3pw6sMCLI6/Hwo0LVwoMh2a1zjG/\n" +
                "zWqnW8BH2ZXXImM+FRl8HlHK8Sil8bV8n4rXEPAmkEwnOURD0HJUe1xcpaKhHT+t\n" +
                "CjHqSFzP/g5mvcp4hZ5Vb9IgURhYT5+olFow9gODtqrYzojBWwL48jZVK5hYxr/d\n" +
                "xR+ie/4YgRNBHho06jHjas7my4GIjgGdGDkMWiieeQvdN37E2G48y+SA8SyYetba\n" +
                "izw2kEyxQSyNUHyttMh8z1KbVv7jB6ZThHsiSBBlNs3AMKBzIQ==\n" +
                "=VM82\n" +
                "-----END PGP PUBLIC KEY BLOCK-----");
        cryptoConfiguration.setPassword("123456");
        cryptoConfiguration.setPrivateKey("-----BEGIN PGP PRIVATE KEY BLOCK-----\n" +
                "\n" +
                "lQPGBF/PUJcBCACfadnXlu2VU2yQsZUjqxQQ+ql59U7J2AGK2Kg0wUmYoocapNA4\n" +
                "Q4ILgYUmYn601f2uANmTxLwHXUGdpAO+yOIRKA7Z9PjP5+Qad8TXnyZ+ZH70HFXi\n" +
                "EwCz8r5e2tGT0JmXVxD+hBKwYihCBB5V21L3+FJYGpyWcJ+hRbs5Dyq5oIrk+DQL\n" +
                "BYbVXSYcUnNyBFdquuIAD4klwqTXjXswhfSObsX82vp+poEiJZ5kNxsw2BBD3PcR\n" +
                "Ma3uYuEtezT2uLR0K/D1jDWdXlZgwiWg2W2Pj8WvxTwV3FcE8Bz539uPgUKtMkgv\n" +
                "a323Unw+/UUkVhjRhRz1xq+dCgd8/WFFj7fXABEBAAH+BwMCPBnwiwriXd3quikx\n" +
                "S1gK2VAMiJz3kspzp1WA492IUPRqDtN6blxgDwjMep4XxGXzXKIkpSOsPP6nl/Vt\n" +
                "CLkXV36Z3DD64uGhnoJixu1E47L5M0VKy9bqzhQyrREm/JTsnqUksxEPukqlHSnI\n" +
                "p/zsjlDFW4lV54yZbwMlXFJheniK+8pfMTb0kAfsv2Kw+xHOLCtqqDHRwdkDXNxT\n" +
                "Q/faJjMgj03ellG/OIZpOINeCOhwH5fH67+1W6g6y0SdN/6qKSaIE5OBUFDLTb/U\n" +
                "YNCEuABzN35UebVlXcEKI1CUgpQXQB7fqRAzqefHBu3yLQAYhHir6Nmtv8nPoAIR\n" +
                "Kg5kgmPk+bLVmRFZQ3T58YUAcdHJoyvrnFMIQUnPokhyearnO2cp3mAF4XfQbyix\n" +
                "rmOrZJMUbpfZWzovzF9coVYEVIVr1WRSif48HANJG9gFrVCiPTv1dfjncrCk9aIg\n" +
                "4urVtAtxlDXL6J182hJorcFHx1sWigqrKm52Fbstv16p29adNjOoN3DDUae3pm/k\n" +
                "SZo9Zjb914aqgi7YMTwyUAaPNMhCjfNHP1skqCATNIEjPvKB/RXQ2ICcyWiFZ0Bv\n" +
                "ktRXE8BcnaG0NKSkMtQt8wRon07DBzTf1UmsG7guH1mk23CVrtdWxoKsATOXTw8O\n" +
                "dldnUUV/o2tTVlud/bCUMOOmZYQtX9U/EbGfLiXPuMr5+usjjmRTv0ZuaUfHIvGK\n" +
                "zj1y4WFDcgpstThlDVhXNKZmN5J6flGMi6mCQ4g/3WUWEHLJgyjaXi8P01BheEKt\n" +
                "4FU9eg5UVRINDJr8N4hUW0LJRt20wKx9PV9RArxQrnsCMte5xY9d6++ORtyELiH7\n" +
                "FpHX+dGiZNtYRMh6bCVKwzkxvgkevz94Azo9ZNFbkRW6tD7bWUU8wMncoBiYwvDU\n" +
                "EJKjqNgmCmmYtB1Kb2huRG9lIDxqb2huZG9lQGV4YW1wbGUuY29tPokBVAQTAQgA\n" +
                "PhYhBKgTWxgG/2oX4pYoJ3j2xzI/qGlRBQJfz1CXAhsDBQkHhh7sBQsJCAcCBhUK\n" +
                "CQgLAgQWAgMBAh4BAheAAAoJEHj2xzI/qGlRCbEIAIvNMDNHyY4vj6igU8S39Tcr\n" +
                "6uPJsASKMJQYSpGn35T0xD8I2X73z5FSgJBCiX8YrMsxfzlQZaNGCL9g/gzyONKu\n" +
                "CASvWf9jaEmKhPJlxpWp6eaBWriZ0H5Z3aAdE4myZSd8QybRrSymGvZoCOpH/5Ho\n" +
                "OeDn3BYInXp6OSz5QHJP4/68xOI6///wtbCYcIVI3nSkiawT4008zU1Fgupnt8Fv\n" +
                "TvLEwQbHESEilaOb3K9q16t5SxIYuHwN8FPGGw8hhNC8AHZjXkxRjw/JRRKHKc8h\n" +
                "R0qFVNBtq7Boc91Uyv7yQ7g+XMn5FHjGIeMgJpOs9lR/biwtEmYFvmeE0OIOh5yd\n" +
                "A8YEX89QlwEIALR2KDh+cj4FQZBcSTfxSes11PRIucjfWVMcSunih908ief6QUrv\n" +
                "LB0UwtbCuXKXvMpkcBGeY8NJE6kLWugw2tvS6vNXuekZwxGSkMYLlsWsRqzKjvZs\n" +
                "qcPPYusuVlzYFfURJu+l6+CJiIQBPpiir0M/CgYZoKQ4+6qnDTD819pZStHiZ6Ye\n" +
                "ccZoPu4dxP/nJHNFiixoEYGkHYzaMVe4i0mHjYdv01s01DeGQmfa/uDWwfhYEUu/\n" +
                "/DafXv+CCQ3Dtsm4oLO7HG2y2DkW7MafhCJ3eUmj/UUqjnCh8RysCYcUDmvSOgz5\n" +
                "StwgxKG5yUO/+B98n7E2Nvhq3lHbSn+2ci0AEQEAAf4HAwJKOBvAzzV4j+pmc4ZF\n" +
                "3UuLQxmEWqanf/As6R4erLCq7CoXB1/yCZJZNSqSe7puIJt4NVt7HOaSOvVNWRt4\n" +
                "tZi9awCsxsxqD2qznEtIpjWr81+hMWrQQhVrZT+rAjVdF4ap94MOm58dL8+z4E5q\n" +
                "HFxNO8jetDs25+MQqQ3+cWZaQlqoZzvCaEEgBjHqBEQrQckkUnGQ4Ucc62ArqYuh\n" +
                "ckTFzPUagS7akFW2cMmKcyg945lZoilJsbzWYpzcmTlrYiKRfcAoQ4ALOVnNxn4v\n" +
                "5v7OrjBRZ7j4+R2YQthwbMFYX3bojCqgwNfwMRUflFQnhXyPpW2luMqsLtWHJYIY\n" +
                "PIryvpye1BYL5YQ+g2fBoS/SShzIxZKX+sKKv05aTe4ewCNbJoA/dc2LuyEffoCy\n" +
                "Iyzc9Ml1HqMoVYVvw9dW1l8w1Vi6DhBXyytf3rVVoWZc02yGUH53J/VqAwdv/2jl\n" +
                "l/5uNQzXv5cvZyj9zaU4X9+AfsFfY8Aw36dlYEg8Yk5UEKHA3alWcwtsPMOJzvgM\n" +
                "ptGbUT1aUZhYEExsCi4wyhRxWYmh3ACmwjDuRJGKvuw470/Q+CMJ10q0CKPhksIe\n" +
                "Wj174TMpANCTQM1ZoMRllK+OBLXS0OcVm8BurR0kSZFvR6XGQggcb8o3D77HhOzK\n" +
                "/lQ7RLgnnWekHgk3CjhBjlJmOqRPowskSoq+RxDhdrDbpfiuneEcAYoiKKDHPzl6\n" +
                "ONmcpWAt+PxI2uXCTLGq20/D4I+QdGXo3IBmXnBYMP84Tkme4cyZEVGuVotrtOdJ\n" +
                "ZYYuwX17TJM5/tBB3wT1EpPo0qq9R/oto0eb8XJnO1LRmnwi0YzWQOQcxESTdmlK\n" +
                "pnzMfl2MEprPvk+gzDG7PISx4JpqH11ove8cC8YZDiitbL05eDUiyiOK25zbFcUN\n" +
                "UA+wpPT86OKJATwEGAEIACYWIQSoE1sYBv9qF+KWKCd49scyP6hpUQUCX89QlwIb\n" +
                "DAUJB4Ye7AAKCRB49scyP6hpURfpB/9+tGZWwKGyhY/+F/N09Kgd8DbuuSqgBomR\n" +
                "1o9w3zl1CBDkCTvwm2knvb1wlQjpMhEsITgz4ajenDqwwIsjr8fCjQtXCgyHZrXO\n" +
                "Mb/NaqdbwEfZldciYz4VGXweUcrxKKXxtXyfitcQ8CaQTCc5REPQclR7XFyloqEd\n" +
                "P60KMepIXM/+Dma9yniFnlVv0iBRGFhPn6iUWjD2A4O2qtjOiMFbAvjyNlUrmFjG\n" +
                "v93FH6J7/hiBE0EeGjTqMeNqzubLgYiOAZ0YOQxaKJ55C903fsTYbjzL5IDxLJh6\n" +
                "1tqLPDaQTLFBLI1QfK20yHzPUptW/uMHplOEeyJIEGU2zcAwoHMh\n" +
                "=iPZe\n" +
                "-----END PGP PRIVATE KEY BLOCK-----");
        cryptoConfiguration.setSalt("5558");

        return cryptoConfiguration;
    }

    public static String extractStringWithinQuotes(String input) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return ""; // Or return null, or throw an exception, depending on what's appropriate in your context.
        }
    }

    public static boolean isValidRegex(String regex) {
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return false;
        }
        return true;
    }

    public static String wildcardRegex(String wildcard) {
        return wildcard.replace("*", ".*");
    }
}
