package com.bank.test;

import com.bank.ui.BankApplicationCLI;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Automated end-to-end test simulating interactive user sessions across all CLI menu flows.
 */
public class CLIIntegrationTest {

    public static void main(String[] args) {
        System.out.println(">>> TESTING INTERACTIVE CLI END-TO-END WORKFLOW");

        // Sequence of simulated inputs:
        // 1: Register customer -> "Dev Sharma" -> "dev@example.com" -> "9876500000" -> ENTER
        // 2: Open Savings -> "CUST-1004" -> "5000" -> "0.04" -> ENTER
        // 3: Open Current -> "CUST-1004" -> "10000" -> "20000" -> ENTER
        // 4: Deposit -> "SAV-100003" -> "2500" -> "Freelance Income" -> ENTER
        // 5: Withdraw -> "SAV-100003" -> "1000" -> ENTER
        // 6: Transfer -> "SAV-100003" -> "CUR-500002" -> "500" -> "Family support" -> ENTER
        // 7: View Statement -> "SAV-100003" -> ENTER
        // 8: View Profile -> "CUST-1004" -> ENTER
        // 10: Exit
        String simulatedInput = String.join("\n",
                "1", "Dev Sharma", "dev@example.com", "9876500000", "",
                "2", "CUST-1004", "5000", "0.04", "",
                "3", "CUST-1004", "10000", "20000", "",
                "4", "SAV-100003", "2500", "Freelance Income", "",
                "5", "SAV-100003", "1000", "",
                "6", "SAV-100003", "CUR-500002", "500", "Family support", "",
                "7", "SAV-100003", "",
                "8", "CUST-1004", "",
                "10"
        ) + "\n";

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));
            BankApplicationCLI.main(new String[0]);
            System.out.println("\n[PASS] CLI Integration Test executed and terminated gracefully!");
        } finally {
            System.setIn(originalIn);
        }
    }
}
