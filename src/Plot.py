import matplotlib.pyplot as plt
import matplotlib.patches as patches

def plot_coordinates(file_path):
    # Read coordinates from the file
    with open(file_path, 'r') as file:
        lines = file.readlines()

    # Extract x and y coordinates
    x_coordinates = []
    y_coordinates = []
    for line in lines:
        x, y = map(float, line.strip().split(","))
        x_coordinates.append(x)
        y_coordinates.append(y)

    # Plot the coordinates
    plt.scatter(x_coordinates, y_coordinates, marker='o', label='Coordinates')
    plt.scatter(55, 47, marker="x", c="red")
    plt.scatter(18, 10, marker="x", c="green")
    plt.scatter(8, 15, marker="x", c="green")

    circle = plt.Circle((55,47), 60/1.1, color='r', fill=False, linestyle='dashed', label='Circle')
    plt.gca().add_patch(circle)

    square1 = patches.Rectangle((18 - square_size / 2, 10 - square_size / 2), square_size, square_size,
                               linewidth=2, edgecolor='g', fill=False, linestyle='dashed')
    plt.gca().add_patch(square1)

    square2 = patches.Rectangle((8 - square_size / 2, 15 - square_size / 2), square_size, square_size,
                               linewidth=2, edgecolor='g', fill=False, linestyle='dashed')
    plt.gca().add_patch(square2)
    
    # Set plot limits
    plt.xlim(0, 100)
    plt.ylim(0, 100)

    # Add labels and title
    plt.xlabel('X-axis')
    plt.ylabel('Y-axis')
    plt.title('Scatter Plot of Coordinates')

    # Add legend
    plt.legend()

    # Display the plot
    plt.show()

if __name__ == "__main__":
    # Specify the file path
    file_path = "Metropolica_coordinates.txt"

    square_size = 15
    # Call the plot_coordinates function
    plot_coordinates(file_path)
