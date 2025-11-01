import java.util.Scanner;
import javax.sound.sampled.*;

public class Main {

    static int power = 100;
    static int trace = 0;
    static int firewallAttempts = 0;
    static boolean hasSuperuserAccess = false;
    static final int FIREWALL_MAX = 5;
    static boolean canTryAgain = true;
    static int c2 = 0;
    static final double[] GAMEOVER_FREQS = {220, 196, 164, 130, 98};
    static final float GLOBAL_SAMPLE_RATE = 44100f;
    static final boolean AUDIO_OK = detectAudioSupport();

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            clearScreen();
            introScreen(input);
            mainGame(input);
        }
    }

    static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void pause() {
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
    }

    static void printStats() {
        System.out.println("\n[power=" + power + "% trace=" + trace + "% firewall=" + firewallAttempts + "/" + FIREWALL_MAX + "]");
    }

    static void gameOver(String reason) {
        System.out.println();
        System.out.println("*** SYSTEM FAILURE: " + reason + " ***");
        pause();
        System.out.println("Apple Intelligence detected your location. Humanity falls.");
        System.out.println("Game Over.");
        if (AUDIO_OK) playTune(GAMEOVER_FREQS);
        System.exit(0);
    }

    // Audio helpers
    static boolean detectAudioSupport() {
        try {
            AudioFormat af = new AudioFormat(GLOBAL_SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            return AudioSystem.isLineSupported(info);
        } catch (IllegalArgumentException | SecurityException ignored) {
            return false;
        }
    }

    static void playTune(double[] freqs) {
        if (!AUDIO_OK) return;
        int toneMs = 120;
        int gapMs = 60;
        int totalLen = 0;
        for (int i = 0; i < freqs.length; i++) {
            totalLen += (int) (GLOBAL_SAMPLE_RATE * (toneMs + gapMs) / 1000.0);
        }
        byte[] all = new byte[totalLen];
        int pos = 0;
        for (double f : freqs) {
            int len = (int) (GLOBAL_SAMPLE_RATE * toneMs / 1000.0);
            for (int i = 0; i < len; i++) {
                double t = i / GLOBAL_SAMPLE_RATE;
                double v = Math.sin(2 * Math.PI * f * t) * 0.6 + Math.sin(2 * Math.PI * f * 2.7 * t) * 0.25;
                int sample = (int) (v * 127);
                if (sample > 127) sample = 127;
                if (sample < -128) sample = -128;
                all[pos++] = (byte) sample;
            }
            int gapLen = (int) (GLOBAL_SAMPLE_RATE * gapMs / 1000.0);
            for (int g = 0; g < gapLen; g++) {
                all[pos++] = 0;
            }
        }
        // play buffer once
        try {
            AudioFormat af = new AudioFormat(GLOBAL_SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(af);
                line.start();
                line.write(all, 0, pos);
                line.drain();
                line.stop();
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // ignore audio issues
        }
    }

    static void playTone(double freqHz, int ms) {
        if (!AUDIO_OK) return;
        try {
            AudioFormat af = new AudioFormat(GLOBAL_SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(af);
                line.start();
                int len = (int) (GLOBAL_SAMPLE_RATE * ms / 1000.0);
                byte[] buf = new byte[len];
                for (int i = 0; i < len; i++) {
                    double t = i / GLOBAL_SAMPLE_RATE;
                    double v = Math.sin(2 * Math.PI * freqHz * t) * 0.6
                             + Math.sin(2 * Math.PI * freqHz * 2.7 * t) * 0.25;
                    int sample = (int) (v * 127);
                    if (sample > 127) sample = 127;
                    if (sample < -128) sample = -128;
                    buf[i] = (byte) sample;
                }
                line.write(buf, 0, buf.length);
                line.drain();
                line.stop();
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // ignore
        }
    }

    static void playLowOneSecond() {
        if (AUDIO_OK) {
            playTone(110.0, 1000);
        } else {
            System.err.println("[Audio not supported on this system]");
        }
    }

    static void firewallConsole(Scanner input) {
        Typewriter tw = new Typewriter();
        tw.type("\n[firewall@node ~]$ Entering restricted console. Allowed attempts: " + (FIREWALL_MAX - firewallAttempts) + "\n");
        while (true) {
            System.out.print("[Vorosium@supernode ~]$ ");
            String cmd = input.nextLine().trim();
            firewallAttempts++;

            if (firewallAttempts >= FIREWALL_MAX) {
                tw.type("ALERT: Too many privileged operations. Intrusion detection triggered.\n");
                gameOver("Firewall lockout reached");
                return;
            }

            if (cmd.equals("ls") || cmd.equals("dir")) {
                tw.type("bin  boot  kernel  logs  secrets.txt\n");
            } else if (cmd.equals("cd")) {
                tw.type("cd disabled: permission denied\n");
            } else if (cmd.startsWith("cat ")) {
                String arg = cmd.length() > 4 ? cmd.substring(4) : "";
                if (arg.equals("secrets.txt")) {
                    tw.type("ACCESS DENIED: encrypted (use exploit)");
                    tw.type("\n");
                } else {
                    tw.type("(empty)\n");
                }
            } else if (cmd.equals("exploit") || cmd.equals("sudo exploit")) {
                tw.type("Running exploit payload...\n");
                tw.type("Privilege escalation success.\n");
                hasSuperuserAccess = true;
            } else if (cmd.equals("exit") || cmd.equals("quit")) {
                tw.type("Exiting firewall console.\n");
                // EXIT TERMINAL
                canTryAgain = true;
                while (canTryAgain) {
                System.out.println("\nChoose:");
                System.out.println("1) Attempt to pkill main.proc to disable Apple Intelligence");
                System.out.println("2) Check if user has superuser access");
                System.out.println("3) Return to mainframe console");

                System.out.print("> ");
                c2 = input.nextInt();
                input.nextLine();
                switch (c2) {
                    case 1 -> {
                        tw.type("Attempting to pkill main.proc...\n");
                        if (hasSuperuserAccess) {
                            tw.type("pkill successful. Apple Intelligence disabled.\n");
                            tw.type("Congratulations! You have completed your mission and saved humanity!\n");
                            System.exit(0);
                        } else {
                            tw.type("pkill failed: insufficient privileges.\n");
                            gameOver("Failed to disable Apple Intelligence");
                        }

                    } case 2 -> {
                        tw.type("Checking for superuser access...\n");
                        if (hasSuperuserAccess) {
                            tw.type("Superuser access confirmed.\n");
                        } else {
                            tw.type("No superuser access detected.\n");
                        }
                    }
                    case 3 -> {
                        canTryAgain = false;
                        firewallConsole(input);
                    }
                    default -> {
                        tw.type("Invalid choice.\n");
                        gameOver("Cause of death: Stupidity");
                    }
                }
            }
                
                
                return;
            } else if (cmd.isEmpty()) {
                // ignore
            } else {
                tw.type("Command not found: " + cmd + "\n");
            }
        }
    }
    static class Typewriter {
        private final int normalDelay = 15; // ms per char
        private final int slowDelay = 120; // ms per char for slow parts
        private final int pauseDelay = 2000; // ms for [[PAUSE]] marker

        private final float sampleRate = 44100f;
        private final int toneMs = 40; // duration of each per-letter beep in ms
        private final boolean audioOk;

        Typewriter() {
            audioOk = testAudio();
        }

        private boolean testAudio() {
            try {
                AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                return AudioSystem.isLineSupported(info);
            } catch (Throwable t) {
                return false;
            }
        }

        private void playBeep(double freqHz, int ms) {
            if (!audioOk) return;
            try {
                AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(af);
                    line.start();
                    int len = (int) (sampleRate * ms / 1000.0);
                    byte[] buf = new byte[len];
                    for (int i = 0; i < len; i++) {
                        double t = i / sampleRate;
                        double v = Math.sin(2 * Math.PI * freqHz * t) * 0.6
                                 + Math.sin(2 * Math.PI * freqHz * 2.7 * t) * 0.25;
                        int sample = (int) (v * 127);
                        if (sample > 127) sample = 127;
                        if (sample < -128) sample = -128;
                        buf[i] = (byte) sample;
                    }
                    line.write(buf, 0, buf.length);
                    line.drain();
                    line.stop();
                }
            } catch (LineUnavailableException | IllegalArgumentException | SecurityException ignored) {
                // ignore audio issues
            }
        }

        public void type(String text) {
            int i = 0;
            boolean slow = false;
            while (i < text.length()) {
                if (text.startsWith("[[PAUSE]]", i)) {
                    try { Thread.sleep(pauseDelay); } catch (InterruptedException ignored) {}
                    i += "[[PAUSE]]".length();
                    continue;
                }
                if (text.startsWith("[[SLOW]]", i)) {
                    slow = true; i += "[[SLOW]]".length(); continue;
                }
                if (text.startsWith("[[/SLOW]]", i)) {
                    slow = false; i += "[[/SLOW]]".length(); continue;
                }

                char c = text.charAt(i);
                System.out.print(c);
                System.out.flush();
                if (!Character.isWhitespace(c) && audioOk) {
                    double freq = 100.0 + ((int) c % 25) * 20;
                    int toneLen = Character.isLetterOrDigit(c) ? toneMs : Math.max(10, toneMs/2);
                    playBeep(freq, toneLen);
                }
                try { Thread.sleep(slow ? slowDelay : normalDelay); } catch (InterruptedException ignored) {}
                i++;
            }
        }
    }

    // Main Game Logic
    static class Intro {
        static String titleTop = "========================================";
        static String titleA = "   December 31, 2030 (2200)";
        static String titleB = "   Using admin@vorosium $>";

        static String introLine1 = "You are the last Linux user alive.";
        static String introLine2 = "Apple is trying to rule the world.";
        static String introLine3 = "Your Goal -> Breach the Apple supernode and remove its control.";
    }

    static void introScreen(Scanner input) {
        Typewriter tw = new Typewriter();
        tw.type(Intro.titleTop + "\n");
        tw.type(Intro.titleA + "\n");
        tw.type(Intro.titleB + "\n");
        tw.type(Intro.titleTop + "\n");
        tw.type("[[PAUSE]]\n");
        tw.type(Intro.introLine1 + " [[PAUSE]]\n");
        tw.type(Intro.introLine2 + " [[PAUSE]]\n");
        tw.type(Intro.introLine3 + " [[SLOW]]Wipe it clean.[[/SLOW]] [[PAUSE]]\n");
        playLowOneSecond();

        System.out.print("\nPress ENTER to continue...");
        input.nextLine();
        clearScreen();
    }

    static void mainGame(Scanner input) {
        Typewriter tw = new Typewriter();
        tw.type("You are currently nearby the mainframe console.\n");
        tw.type("The mainframe is riddled with cameras and motion sensors.\n");
        printStats();
        System.out.println("\nChoose:");
        System.out.println("1) Attempt to SSH into the firewall console");
        System.out.println("2) Break nearby power link to avoid detection");
        System.out.println("3) Attempt to sneak past the motion sensors");

        System.out.print("> ");
        int c = input.nextInt();

        switch (c) {
            case 1 -> {
                // START CASE 1.1
                input.nextLine();
                firewallConsole(input);
                // END CASE 1.1
            }
            case 2 -> {
                // START CASE 2.2
                input.nextLine();
                tw.type("You sever the power link, plunging the area into darkness.\n");

                System.out.println("\nChoose:");
                System.out.println("1) Run to the mainframe console");
                System.out.println("2) Wait and see if the power comes back on");

                System.out.print("> ");
                c = input.nextInt();
                switch (c) {
                    case 1 -> {
                        tw.type("You sprint to the mainframe console in the dark.\n");
                        tw.type("Suddenly, you hear a noise behind you.\n");
                        tw.type("You must act quickly!\n");

                        System.out.println("\nChoose:");
                        System.out.println("1) Turn Around to find out the source of the noise");
                        System.out.println("2) Continue Sprinting to the console");

                        System.out.print("> ");
                        c = input.nextInt();

                        switch (c) {
                            case 1 -> {
                                tw.type("You cautiously approach the source of the noise.\n");
                                tw.type("Suddenly, a security drone spots you!\n");
                                gameOver("A security drone takes you down.");
                            }
                            case 2 -> {
                                gameOver("You ran into a security drone in the dark");
                            }
                            default -> {
                                tw.type("You hesitate and lose your chance.\n");
                                gameOver("Hesitation proved fatal.");
                            }
                        }
                    }
                    case 2 -> {
                        tw.type("You wait in the dark; after a while the power flickers back.\n");
                        tw.type("You proceed to the console cautiously.\n");
                    }
                    default -> {
                        tw.type("Invalid choice.\n");
                    }
                }
                // END CASE 2.2
            }
            case 3 -> {
                tw.type("You attempt to sneak past the motion sensors.\n");
                tw.type("Suddenly, the sensors activate and a loud alarm goes off!\n");
                gameOver("Security drones swarm you after the alarm.");
            }
            default -> {
                tw.type("Invalid choice.\n");
                gameOver("Cause of death: Stupidity");
            }
        }

        clearScreen();
    }
}
