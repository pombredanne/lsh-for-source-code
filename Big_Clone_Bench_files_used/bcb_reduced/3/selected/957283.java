package org.rcpquizengine.control.handlers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rcpquizengine.model.Folder;
import org.rcpquizengine.ui.dialogs.PasswordDialog;
import org.rcpquizengine.ui.views.QuizTreeView;

public class UnlockQuizBankHandler extends AbstractHandler implements IHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            QuizTreeView view = (QuizTreeView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.rcpquizengine.views.quizzes");
            Folder rootFolder = view.getRootFolder();
            if (rootFolder.isEncrypted()) {
                PasswordDialog dialog = new PasswordDialog(shell);
                if (dialog.open() == Window.OK) {
                    String password = dialog.getPassword();
                    if (!password.equals("")) {
                        String md5 = "";
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.update(password.getBytes());
                        md5 = new BigInteger(md.digest()).toString();
                        if (rootFolder.getMd5Digest().equals(md5)) {
                            rootFolder.setMd5Digest("");
                            rootFolder.setEncrypted(false);
                            MessageDialog.openInformation(shell, "Quiz bank unlocked", "The current quiz bank has been unlocked");
                        } else {
                            MessageDialog.openError(shell, "Error unlocking quiz bank", "Incorrect password");
                        }
                        password = "";
                        md5 = "";
                    }
                }
            } else {
                MessageDialog.openError(shell, "Error unlocking quiz bank", "Quiz bank already unlocked");
            }
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
