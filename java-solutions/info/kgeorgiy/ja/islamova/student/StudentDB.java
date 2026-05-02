package info.kgeorgiy.ja.islamova.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class StudentDB implements GroupQuery {

    private <T> List<T> getName(List<Student> students,  Function<Student, T> func) {
        return students.stream().map(func).toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getName(students, Student::firstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getName(students, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> students) {
        return getName(students, Student::groupName);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getName(students, student -> student.firstName() + ' ' + student.lastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(students.stream()
                .map(Student::firstName)
                .toList());
    }

    private final Comparator <? super Student> comparingById = // :NOTE: caps
            Comparator.comparing(Student::id);

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(comparingById)
                .map(Student::firstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream()
                .sorted(comparingById)
                .toList();
    }

    private final Comparator <? super Student> comparingByName =
            Comparator.comparing(Student::firstName).thenComparing(Student::lastName).thenComparing(Student::compareTo);

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream()
                .sorted(comparingByName)
                .toList();
    }

    private List<Student> findStudentBy(Collection<Student> students, Predicate<Student> pr) {
        return students.stream()
                .filter(pr)
                .sorted(comparingByName)
                .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentBy(students, student -> Objects.equals(student.firstName(), name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentBy(students, student -> Objects.equals(student.lastName(), name));
    }

    private Stream<Student> getGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> Objects.equals(student.groupName(), group));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return getGroup(students, group)
                .sorted(comparingByName)
                .toList();
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return getGroup(students, group)
                .collect(Collectors.toMap(Student::lastName,
                        Student::firstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private List<Group> getGroupBy(Collection<Student> students, Comparator <? super Student> cmp) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::groupName))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(entry -> new Group(entry.getKey(),
                        entry.getValue().stream()
                                .sorted(cmp)
                                .toList()
                )).toList();
    }
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, comparingByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, comparingById);
    }

    private final Comparator<? super Map.Entry<GroupName, Long>> comparingByNumber =
            Comparator.<Map.Entry<GroupName, Long>>comparingLong(Map.Entry::getValue)
                    .thenComparing(Map.Entry.comparingByKey());

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::groupName, Collectors.counting()))
                .entrySet().stream()
                .max(comparingByNumber)
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private final Comparator<? super Map.Entry<GroupName, Long>> comparingByNumberMin =
            Comparator.<Map.Entry<GroupName, Long>>comparingLong(Map.Entry::getValue)
                    .thenComparing(Map.Entry.<GroupName, Long>comparingByKey().reversed());

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::groupName, Collectors.mapping(Student::firstName, toSet())))
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), (long) entry.getValue().size()))
                .max(comparingByNumberMin)
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
