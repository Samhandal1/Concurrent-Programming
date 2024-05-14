// Samantha Handal - 260983914
#include <omp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define START_STATE 0
#define FIRST_MATCH_STATE 1
#define CONTINUING_STATE 2
#define ACCEPT_STATE 3
#define NUM_STATES 4

// DFA transitions and match count logic
int dfa_transition(int state, char input, int *match_count) {
    switch (state) {

        // Initial state of the DFA
        case START_STATE:
            if (input >= '1' && input <= '9') {
                return FIRST_MATCH_STATE;
            }
            break;

        // Second state, after initial valid digit
        case FIRST_MATCH_STATE:
            if ((input >= '0' && input <= '9') || (input >= 'a' && input <= 'f')) {
                return CONTINUING_STATE;

            // Transition to the accepting state if the input is x or y and increment the match count
            } else if (input == 'x' || input == 'y') {
                if (match_count) *match_count += 1;
                return ACCEPT_STATE;
            }
            break;

        // State where we've seen at least two valid characters
        case CONTINUING_STATE:

            // Stay in the same state
            if ((input >= '0' && input <= '9') || (input >= 'a' && input <= 'f')) {
                return CONTINUING_STATE;

            // Transition to the accepting state if the input is x or y and increment the match count
            } else if (input == 'x' || input == 'y') {
                if (match_count) *match_count += 1;
                return ACCEPT_STATE;
            }
            break;

        // Accepting state, loop back to the start for the next potential match
        case ACCEPT_STATE: 

            // Go to first matching state if we get a valid hexadecimal character
            if ((input >= '0' && input <= '9') || (input >= 'a' && input <= 'f')) {
                return FIRST_MATCH_STATE;

            // Stay in accept state if we get another 'x' or 'y'
            } else if (input == 'x' || input == 'y') {
                return ACCEPT_STATE;
            }
            break;
    }

    // For any other case, or if no valid transition, go back to the start state
    return START_STATE;
}

// Function to count matches in a given segment of the string: Iterate over each character
// in the segment, applying the DFA transitions, and updates the local match count whenever a
// match is found according to the DFA's logic. Then finally set the end state, which is needed
// to handle cases where a match may span across segments processed by different threads.
void count_matches_in_segment(const char *str_segment, int len, int *local_match_count, int *end_state) {
    
    // Each new segment starts with the DFA in the START_STATE
    int state = START_STATE;
    for (int i = 0; i < len; i++) {
        state = dfa_transition(state, str_segment[i], local_match_count);
    }

    // Update the ending state of this segment
    *end_state = state; 
}

// Random string generator function
char* generate_random_string(int n) {
    char* str = malloc(n + 1);
    char charset[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                      'a', 'b', 'c', 'd', 'e', 'f', 'x', 'y'};
    for (int i = 0; i < n; i++) {
        int key = rand() % (sizeof(charset) / sizeof(char));
        str[i] = charset[key];
    }
    str[n] = '\0';
    return str;
}

int main(int argc, char *argv[]) {
    if (argc != 3) {
        printf("Usage: %s <number_of_threads> <size_of_string>\n", argv[0]);
        return 1;
    }

    int num_threads = atoi(argv[1]) + 1;
    int string_size = atoi(argv[2]);
    if (num_threads <= 0 || string_size <= num_threads) {
        printf("Error: Invalid parameters. Make sure that t ≥ 0 and n > t.\n");
        return 1;
    }

    // Seed the random number generator
    srand(time(NULL));

    // Generate the random string
    char* random_string = generate_random_string(string_size);

    // Prepare shared data structures for thread results
    int *counts = calloc(num_threads, sizeof(int));
    int *end_states = calloc(num_threads, sizeof(int));
    
    // Calculate the segment size, the last thread may get a longer segment
    int segment_size = string_size / num_threads;

    // Parallel region starts here
    double start_time = omp_get_wtime();
    #pragma omp parallel num_threads(num_threads)
    {
        int thread_id = omp_get_thread_num();
        int local_match_count = 0;
        int local_end_state = START_STATE;
        
        // Calculate the segment of the string this thread will work on
        int start_index = thread_id * segment_size;
        int end_index = (thread_id == num_threads - 1) ? string_size : start_index + segment_size;
        
        // Thread 0 - Naive matching
        if (thread_id == 0) {
            count_matches_in_segment(&random_string[start_index], end_index - start_index, &local_match_count, &local_end_state);
        
        // Threads 1 to t-1 - Optimistic matching
        } else {
            for (int state = 0; state < NUM_STATES; state++) {
                int temp_match_count = 0;
                local_end_state = state;
                count_matches_in_segment(&random_string[start_index], end_index - start_index, &temp_match_count, &local_end_state);
            }
        }
        
        // Write the local results to the shared arrays
        counts[thread_id] = local_match_count;
        end_states[thread_id] = local_end_state;
    }
    double end_time = omp_get_wtime();

    // Processing the results sequentially to avoid data races and combine the match counts
    // Only keep the counts if the end state matches the start state of the next segment
    int total_matches = counts[0];
    int previous_end_state = end_states[0];

    // Correct the match count by considering the end state of the previous segment
    for (int i = 1; i < num_threads; ++i) {

        // Adjust the match count if the previous thread ended in a match state
        if (previous_end_state == ACCEPT_STATE) {
            total_matches += (counts[i] - 1); 
        } else {
            total_matches += counts[i];
        }
        previous_end_state = end_states[i];
    }

    // Output the results
    printf("%s\n", random_string);
    printf("%d\n", total_matches);
    printf("%.0f milliseconds\n", (end_time - start_time) * 1000);

    // Cleanup
    free(counts);
    free(end_states);
    free(random_string);

    return 0;
}