package dev.noctud.latte.completion.resolvers;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

abstract class PresenterResolver {
    protected final LatteFile file;
    //private static List<PhpClass> presenterCache = null;

    public PresenterResolver(LatteFile file) {
        this.file = file;
    }

    public void reset() {
        //presenters = null;
    }

    public @Nullable PhpClass resolvePresenter(String presenter, List<String> previousPresenters, boolean preferAbstract) {
        String key = presenter + previousPresenters.toString() + preferAbstract;
        /*if (presenterCache.containsKey(key)) {
            return presenterCache.get(key);
        }*/

        PhpClass result = calculatePresenter(presenter, previousPresenters, preferAbstract);

        //presenterCache.put(key, result);
        return result;
    }

    private @Nullable PhpClass calculatePresenter(String presenter, List<String> previousPresenters, boolean preferAbstract) {
        List<PhpClass> matchingPresenters = getMatchingPresenters(presenter, preferAbstract, previousPresenters);

        if (matchingPresenters.isEmpty()) {
            return null;
        }

        return matchingPresenters.get(0);
    }

    protected @Nullable Method findMethod(PhpClass phpClass, List<String> methodNames) {
        for (String methodName : methodNames) {
            Method method = phpClass.findMethodByName(methodName);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    public @Nullable PhpClass findPresenter(List<String> previousPresenters, boolean preferAbstract) {
        List<PhpClass> matching = getMatchingPresenters(null, preferAbstract, previousPresenters);
        if (!matching.isEmpty()) {
            return matching.get(0);
        }

        return null;
    }

    public List<LookupElement> getPresentersForAutoComplete(@Nullable String parent, boolean fullQualified, boolean root) {
        List<PhpClass> presenters = getPresenters();

        PhpClass parentClass = null;
        if (parent != null) {
            parentClass = getMatchingPresenters(parent, true).stream().findFirst().orElse(null);
        } else {
            PhpClass templatePresenter = getMatchingPresenters(null, true).stream().findFirst().orElse(null);
            if (templatePresenter != null) {
                if (templatePresenter.isAbstract() && presenters.contains(templatePresenter)) {
                    parentClass = templatePresenter;
                } else if (templatePresenter.getSuperClass() != null && presenters.contains(templatePresenter.getSuperClass())) {
                    parentClass = templatePresenter.getSuperClass();
                }
            }
        }

        boolean atLeastOneChild = parentClass != null && getSubclassTree(parentClass).size() > 1;

        boolean searchOnlyBaseModules = false;
        if (fullQualified && parent == null) {
            for (PhpClass presenter : presenters) {
                if (presenter.isAbstract() && !getPresenterNameForAutoCompletion(presenter).isEmpty()) {
                    searchOnlyBaseModules = true;
                    break;
                }
            }
        }

        List<LookupElement> result = new ArrayList<>();
        for (PhpClass presenter : presenters) {
            if (parent != null && parentClass != null && presenter.isEquivalentTo(parentClass)) {
                continue;
            }

            String name = getPresenterNameForAutoCompletion(presenter);

            if (searchOnlyBaseModules || root) {
                if (presenter.isAbstract() && !name.isEmpty()) {
                    PhpClass superClass = presenter.getSuperClass();
                    if (superClass == null || !presenters.contains(superClass) || getPresenterNameForAutoCompletion(superClass).isEmpty()) {
                        result.add(LookupElementBuilder
                            .create((root ? ":" : "") + name + ":")
                            .withPresentableText(":" + name)
                            .withTailText(": from " + presenter.getName())
                            .withIcon(AllIcons.Actions.GroupByTestProduction)
                        );
                    }
                }
            }

            if (atLeastOneChild) {
                PhpClass superClass = presenter.getSuperClass();

                if (superClass != null && superClass.isEquivalentTo(parentClass) && !name.isEmpty()) {
                    result.add(LookupElementBuilder
                        .create(name + ":")
                        .withPresentableText(name)
                        .withTailText(": from " + presenter.getName())
                        .withIcon(presenter.isAbstract() ? AllIcons.Actions.GroupByTestProduction : AllIcons.Nodes.Property)
                    );
                }

            }

            if (!atLeastOneChild || root) {
                if (parentClass != null) {
                    if (presenter.getSuperClass() != null && presenter.getSuperClass().isEquivalentTo(parentClass)) {
                        result.add(LookupElementBuilder
                            .create(name + ":")
                            .withPresentableText(name)
                            .withTailText(": from " + presenter.getName())
                            .withIcon(presenter.isAbstract() ? AllIcons.Actions.GroupByTestProduction : AllIcons.Nodes.Property)
                        );
                    }

                } else {
                    result.add(LookupElementBuilder
                        .create(name + ":")
                        .withPresentableText(name)
                        .withTailText(": from " + presenter.getName())
                        .withIcon(presenter.isAbstract() ? AllIcons.Actions.GroupByTestProduction : AllIcons.Nodes.Property)
                    );
                }
            }
        }

        return result;
    }

    private String getPresenterNameForAutoCompletion(PhpClass presenter) {
        if (presenter.isAbstract()) {
            return presenter.getName().replace("Abstract", "").replace("Base", "").replace("Presenter", "");
        }

        return presenter.getName().replace("Presenter", "");
    }

    protected List<PhpClass> getMatchingPresenters(String name, boolean preferAbstract) {
        return getMatchingPresenters(name, preferAbstract, new ArrayList<>());
    }

    protected List<PhpClass> getMatchingPresenters(@Nullable String name, boolean preferAbstract, List<String> previousPresenters) {
        List<String> presenterNames = new ArrayList<>();
        if (name == null) {
            presenterNames.addAll(guessPresenterNames());
        } else {
            presenterNames.add(name);
        }

        List<PhpClass> presenters = getPresenters();

        HashMap<String, PhpClass> candidates = new HashMap<>();
        HashMap<String, Integer> namespaceScore = new HashMap<>();
        HashMap<String, Integer> distanceScore = new HashMap<>();

        String templatePath = null;
        try {
            templatePath = file.getOriginalFile().getContainingDirectory().getVirtualFile().getPath();
        } catch (Exception ignored) {
        }

        for (String presenterName : presenterNames) {
            for (PhpClass presenter : presenters) {

                // exact FQL match
                if (presenter.getFQN().equals(presenterName)) {
                    return List.of(presenter);
                }

                if (presenter.getName().equals(presenterName + "Presenter") || (presenter.isAbstract() && presenter.getName().contains(presenterName + "Presenter"))) {
                    String fqn = presenter.getFQN();
                    String fqnLower = fqn.toLowerCase();

                    int score = 0;
                    if (previousPresenters != null) {
                        for (String prev : previousPresenters) {
                            if (prev == null) continue;
                            String needle = "\\\\" + prev.toLowerCase();
                            if (fqnLower.contains(needle)) {
                                score += 1; // only +1 per previous presenter name, not per occurrence
                            }
                        }
                    }

                    int distance = Integer.MAX_VALUE;
                    try {
                        String presenterPath = presenter.getContainingFile().getContainingDirectory().getVirtualFile().getPath();
                        if (templatePath != null) {
                            int commonPrefixLength = 0;
                            int maxLength = Math.min(templatePath.length(), presenterPath.length());
                            while (commonPrefixLength < maxLength &&
                                templatePath.charAt(commonPrefixLength) == presenterPath.charAt(commonPrefixLength)) {
                                commonPrefixLength++;
                            }

                            distance = templatePath.length() + presenterPath.length() - 2 * commonPrefixLength;
                        }
                    } catch (Exception ignored) {
                    }

                    if (!candidates.containsKey(fqn)) {
                        candidates.put(fqn, presenter);
                        namespaceScore.put(fqn, score);
                        distanceScore.put(fqn, distance);
                    }
                }
            }
        }

        List<PhpClass> ordered = new ArrayList<>(candidates.values());
        ordered.sort((a, b) -> {
            int scoreA = namespaceScore.getOrDefault(a.getFQN(), 0);
            int scoreB = namespaceScore.getOrDefault(b.getFQN(), 0);
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA); // higher score first
            }
            int distA = distanceScore.getOrDefault(a.getFQN(), Integer.MAX_VALUE);
            int distB = distanceScore.getOrDefault(b.getFQN(), Integer.MAX_VALUE);
            return Integer.compare(distA, distB); // smaller distance first
        });

        if (preferAbstract) {
            List<PhpClass> abstracts = new ArrayList<>();
            for (PhpClass presenter : ordered) {
                if (presenter.isAbstract()) {
                    abstracts.add(presenter);
                }
            }
            if (!abstracts.isEmpty()) {
                return abstracts;
            }
        }

        return ordered;
    }

    protected List<String> guessPresenterNames() {
        List<String> names = new ArrayList<>();

        if (this.file.getFirstLatteTemplateType() != null) {
            String namespace = this.file.getFirstLatteTemplateType().getTypes().get(0);
            names.add(namespace.replaceAll("Template$", "") + "Presenter");
            String[] parts = namespace.split("\\\\");
            String className = parts[parts.length - 1];
            names.add(className.replace("Template", ""));
        }

        PsiDirectory containingDirectory = file.getOriginalFile().getContainingDirectory();
        if (containingDirectory != null) {
            String directoryName = containingDirectory.getName();
            directoryName = directoryName.substring(0, 1).toUpperCase() + directoryName.substring(1);

            if (!names.contains(directoryName)) {
                names.add(directoryName);
            }
        }

        String fileName = file.getOriginalFile().getName().replace(".latte", "");
        fileName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);

        for (String separator : List.of("-", "_")) {
            if (fileName.contains(separator)) {
                names.add(fileName.split(separator)[0]);
            }
        }

        if (!names.contains(fileName)) {
            names.add(fileName);
        }

        return names;
    }

    protected List<PhpClass> getPresenters() {
        PhpClass presenterParent = PhpIndex.getInstance(file.getProject()).getAnyByFQN("\\Nette\\Application\\UI\\Presenter").stream().findFirst().orElse(null);
        if (presenterParent == null) {
            return new ArrayList<>();
        }

        return getSubclassTree(presenterParent);
    }

    private List<PhpClass> getSubclassTree(PhpClass phpClass) {
        List<PhpClass> out = new ArrayList<>();

        for (PhpClass subclass : PhpIndex.getInstance(file.getProject()).getDirectSubclasses(phpClass.getFQN())) {
            out.add(subclass);
            out.addAll(getSubclassTree(subclass));
        }

        return out;
    }
}
