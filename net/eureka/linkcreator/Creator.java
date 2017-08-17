package net.eureka.linkcreator;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Tool used to create a massive amount of soft/hard links on Windows. The user
 * selects directory/s, the file paths within those directories are gathered then
 * the user selects a directory to place the links.
 * <br>
 * <h2>Limitations:</h2>
 * <ul>
 * 		<li>Cannot link folders.</li>
 * 		<li>Hard links can only be placed within the same Disk Drive .</li>
 * </ul>
 * 
 * 
 * @author Owen McMonagle.
 * @version 0.3
 * @since 11/08/2017
 *
 */
public final class Creator 
{
	/**
	 * Used to signify the user input soft or hard links.
	 */
	private static boolean softLinks = true;
	
	/**
	 * Chosen parent directory to reopen after link selection.
	 */
	private static String choosenParentDirectory = "";
	
	/**
	 * Asks users which type of links they wish to create. Then proceeds to 
	 * directory selection.
	 */
	public static void main(String[] args)
	{
		// Ask user type of link.
		startOptionInput();
		// Ask user for directories to create links from.
		startLinkProcess();
	}
	
	/**
	 * Creates an option dialog and asks the user which type of link to create
	 * hard or soft.
	 */
	private static void startOptionInput()
	{
		// Define message strings
		final String title = "Symbolic/Hard Link Creator",
				message = "Would you like softlinks or hardlinks?\nHardlinks can only be used on the same Disk Drive!";
		final String[] options = { "HardLinks", "SoftLinks" };
		
		// Show popup dialog and get input.
		int action = JOptionPane.showOptionDialog(null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		// if action is 0, hard links was chosen...
		if(action == 0)
			softLinks = false;
	}
	
	/**
	 * Creates a dialog asking which directories to scan for links, validates
	 * the input, retrieves the link paths, then creates a second dialog asking
	 * where to place the links. After which the links are created and placed in.
	 */
	private static void startLinkProcess()
	{
		// Signal only one folder to be selected for link placement.
		final boolean multiple_selection_allowed = false;
		// Ask user which directories to create links from.
		// Then return the valid paths inside those folders.
		String[] paths = getUserInput();
		// Ask user which directory to place links.
		File link_directory = selectDirectory("Select a directory to place the linked files...", choosenParentDirectory, multiple_selection_allowed)[0];
		// Create hard/soft links inside a directory that the user selects.
		createLinkedFiles(link_directory, paths);
	}
	
	/**
	 * Creates a dialog asking which directories to scan for links, validates
	 * the input, retrieves the link paths.
	 * @return paths to create links with.
	 */
	private static String[] getUserInput()
	{
		// Allow multiple selection of directories.
		final boolean multiple_selection_allowed = true;
		boolean valid_paths = false, same_drive_error = false;
		File[] directories_to_clone = null;
		String[] paths = null;
		ArrayList<String> drive_letters = null;
		do
		{
			// Ask user to select directory/s.
			directories_to_clone = selectDirectory("Select a directory to link...", "", multiple_selection_allowed);
			// Get paths from directories.
			try
			{
				// Set directory to reopen after verification.
				choosenParentDirectory = directories_to_clone[0].getParent();
				// Get paths from chosen directories.
				paths = getPathsFromDirectories(directories_to_clone);
				// Create List in order to store drive letters.
				drive_letters = new ArrayList<>();
				// Populate list with drive letters from paths.
				extractDriveLetters(paths, drive_letters);
				// Validate selection.
				valid_paths = paths.length > 0;
				if(!softLinks)
					same_drive_error = drive_letters.size() > 1;
			}
			catch(NullPointerException e)
			{
				System.out.println("Invalid selection.");
			}
			valid_paths = errorChecks(valid_paths, same_drive_error);
		} 
		while (!valid_paths);
		return paths;
	}
	
	/**
	 * Creates a {@link JFileChooser} dialog to search for directories. Returns
	 * selected directories.
	 * @param title - Dialog title.
	 * @param dir - Directory to start in.
	 * @param multiple_selection - Allow multiple selection or not.
	 * @return Selected directories.
	 */
	private static File[] selectDirectory(String title, String dir, boolean multiple_selection)
	{
		if(title != null && dir != null)
		{
			JFileChooser file_chooser = new JFileChooser(dir);
			file_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			file_chooser.setMultiSelectionEnabled(multiple_selection);
			file_chooser.setDialogTitle(title);
			final int action = file_chooser.showSaveDialog(null);
			if(action == JFileChooser.APPROVE_OPTION)
			{
				File[] directories = null;
				if(multiple_selection)
					directories = file_chooser.getSelectedFiles();
				else
					directories = new File[]{file_chooser.getSelectedFile()};
				
				if(isAllDirectories(directories))
					return directories;
			}
			else if (action == JFileChooser.CANCEL_OPTION)
				System.exit(0);
		}	
		return null;
	}
	
	/**
	 * Checks for errors given the passed parameters. Returns whether or not
	 * paths are still valid. 
	 * @param valid_paths - valid number of paths.
	 * @param same_drive_error - valid number of drives.
	 * @return True for valid paths, false otherwise.
	 */
	private static boolean errorChecks(boolean valid_paths, boolean same_drive_error)
	{
		String error_msg = "";
		
		// If the paths are invalid.
		if(!valid_paths)
			// Store invalid path error message.
			error_msg = "No paths or invalid paths selected!";
		
		// If two drives exist in the selection with hard links enabled...
		if(same_drive_error)
		{
			// Store same drive error message.
			error_msg = "Hard links can only be placed on the same drive!";
			valid_paths = false;
		}
		
		// Show error message.
		if(!valid_paths)
			JOptionPane.showMessageDialog(null, error_msg, "Invalid Selection", JOptionPane.INFORMATION_MESSAGE);
		
		return valid_paths;
	}
	
	/**
	 * Extracts drive letter from paths passed and populates the passed array list. 
	 * @param paths - Paths to create links from.
	 * @param to_populate - List to populate with drive letters.
	 */
	private static void extractDriveLetters(String[] paths, ArrayList<String> to_populate)
	{
		if(paths != null && to_populate != null)
		{
			// Clear list of previous drive letters.
			to_populate.clear();
			// String to store parsed drive letter.
			String sub_str = "";
			// Iterate over paths...
			for(String path : paths)
				// If the path is not null, not empty and the list does not 
				// already contain the drive letter...
				if(path != null && !path.isEmpty() && !to_populate.contains((sub_str = path.substring(0, 1))))
					// Then add to the drive letter list.
					to_populate.add(sub_str);
		}
	}
	
	/**
	 * Checks if the passed files are directories.
	 * @param directories - files to validate.
	 * @return True if all are directories, false otherwise.
	 */
	private static boolean isAllDirectories(File[] directories) 
	{
		if(directories != null)
			// Iterate over directories...
			for(File directory : directories)
				// if file is not a directory return false.
				if(!directory.isDirectory())
					return false;
		return true;
	}
	
	/**
	 * Creates hard or soft links with the passed parameters. Uses Command Prompt
	 * on Windows in order to create the links. Shows information dialog once 
	 * completed. 
	 * @param directory - Folder to place links.
	 * @param paths - Paths which represent the links to be created.
	 */
	private static void createLinkedFiles(File directory, String[] paths)
	{
		if(directory != null && paths != null)
		{
			// Create mklink command string for use in CMD.
			// Set either hard of soft links here.
			String command  = "mklink "+((softLinks) ? "" : "/H" )+" \"",
					// String to join command and path.
					joined_path_command = "", 
					// Directory to place files.
					directory_path = "",
					// Name of the file to link.
					name = "";
			
			// If the File passed is a directory and the paths is greater than 
			// zero...
			if(directory.isDirectory() && paths.length > 0)
			{
				// Get folder path from file.
				directory_path = directory.getAbsolutePath();
				// Iterate over paths to create links from...
				for(String path : paths)
					try 
					{
						// Get name of file to link.
						name = path.substring(path.lastIndexOf("\\"));
						// Parse together command.
						joined_path_command = command + directory_path+name+"\" \"" + path + "\"";
						// Execute command to create hard/soft link.
						Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c",joined_path_command});
					}
					catch (Exception e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				// Show message at the end showing completion.
				JOptionPane.showMessageDialog(null, paths.length + " paths processed.", "Completed", JOptionPane.INFORMATION_MESSAGE);
			}
		}	
	}
	
	/**
	 * Retrieves the paths of all files within the passed directories. Only files
	 * are picked up, directories are not supported at this time.
	 * @param directories - Folders to retrieve paths from.
	 * @return Paths gathered. Returns Null if the directories is Null or if the
	 * number of directories is zero.
	 */
	private static String[] getPathsFromDirectories(File[] directories)
	{
		if(directories != null)
		{
			// Temporary List to store file paths.
			ArrayList<String> paths = new ArrayList<String>();
			// If the number of directories is greater than zero...
			if(directories.length > 0)
			{
				// Iterate over directories...
				for(File directory : directories)
				{
					// Get valid paths from directory.
					String[] paths_from_dir = getPathsFromDirectory(directory);
					if(paths_from_dir != null)
						// Iterate over files gathered..
						for(String path : paths_from_dir)
							// adding each to the list.
							paths.add(path);
				}
				// Return list as array.
				return paths.toArray(new String[paths.size()]);
			}
		}
		return null;
	}
	
	/**
	 * Retrieves the absolute path of all file within a single directory that
	 * is passed.
	 * @param directory - Folder to retrieve paths from.
	 * @return Paths gathered from folder. Returns null if directory is null
	 * or if the number of files in the directory is zero.
	 */
	private static String[] getPathsFromDirectory(File directory)
	{
		if(directory != null)
		{
			// Get files from directory
			File[] files = directory.listFiles();
			// If the number of files is greater that zero...
			if(files.length > 0 )
			{
				// Temporary List to store file paths.
				ArrayList<String> file_paths = new ArrayList<>();
				// Iterate over files...
				for(int i = 0; i < files.length; i++)
					// Confirm file is not a directory...
					if(!files[i].isDirectory())
						// If so store path in list.
						file_paths.add(files[i].getAbsolutePath());
				
				// Return list as array.
				return file_paths.toArray(new String[file_paths.size()]);
			}
		}
		return null;
	}
}
