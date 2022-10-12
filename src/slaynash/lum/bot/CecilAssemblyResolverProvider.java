package slaynash.lum.bot;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mono.cecil.AssemblyDefinition;
import mono.cecil.AssemblyNameReference;
import mono.cecil.IAssemblyResolver;
import mono.cecil.IAssemblyResolverProvider;
import mono.cecil.ModuleDefinition;
import mono.cecil.ReaderParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CecilAssemblyResolverProvider implements IAssemblyResolverProvider {
    @Override
    public @NotNull IAssemblyResolver createResolver() {
        return new AssemblyResolver();
    }


    public static class AssemblyResolver implements IAssemblyResolver {

        private final List<AssemblyDefinition> assemblydefs = new ArrayList<>();
        private final List<File> directories = new ArrayList<>();

        @Override
        public Iterator<AssemblyDefinition> iterator() {
            return assemblydefs.iterator();
        }

        @Override
        public void dispose() {
        }

        @Override
        public void addSearchDirectory(File directory) {
            directories.add(directory);
        }

        @Override
        public void removeSearchDirectory(File directory) {
            directories.remove(directory);
        }

        @Override
        public boolean registerAssembly(AssemblyDefinition arg0) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public AssemblyDefinition resolve(AssemblyNameReference name, @Nullable ReaderParameters parameters) {
            if (name == null)
                throw new NullPointerException();
            if (parameters == null)
                throw new NullPointerException();

            return searchDirectory(name, directories, parameters);
        }

        @Override
        public AssemblyDefinition resolve(String name, ReaderParameters arg1) {
            //return resolve(name, parameters);
            throw new NotImplementedException("resolve(string, ReaderParameters) is not implemented");
        }


        protected AssemblyDefinition searchDirectory(AssemblyNameReference name, Iterable<File> directories, ReaderParameters parameters) {
            for (File directory : directories) {
                File file = Path.of(directory.getAbsolutePath(), name.getName() + ".dll").toFile();
                if (!file.exists())
                    continue;

                return getAssembly(file.getAbsolutePath(), parameters);
            }

            return null;
        }

        AssemblyDefinition getAssembly(String file, ReaderParameters parameters) {
            if (parameters.getAssemblyResolver() == null)
                parameters.setAssemblyResolver(this);

            return ModuleDefinition.readModule(file, parameters).getAssembly();
        }
    }
}
