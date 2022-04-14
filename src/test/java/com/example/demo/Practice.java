package com.example.demo;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


class Person{

    Person(String name, String address, String number)
    {
        this.name = name;
        this.address = address;
        this.number = number;
    }
    Person(Person p)
    {
        this.name = p.name;
        this.address = p.address;
        this.number = p.number;
    }

    String name;
    String address;
    String number;
}

@SpringBootTest
public class Practice {

    //1. ["Blenders", "Old", "Johnnie"] 와 "[Pride", "Monk", "Walker"] 를
    // 순서대로 하나의 스트림으로 처리되는 로직 검증
    @Test
    public void concatWithDelay() {
        Flux<String> names1 = Flux.just("Blenders", "Old", "Johnnie")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names2 = Flux.just("Pride", "Monk", "Walker")
                .delayElements(Duration.ofSeconds(1));
        Flux<String> names = Flux.concat(names1, names2)
                .log();

        StepVerifier.create(names)
                .expectSubscription()
                .expectNext("Blenders", "Old", "Johnnie", "Pride", "Monk", "Walker")
                .verifyComplete();
    }

    //2. 1~100 까지의 자연수 중 짝수만 출력하는 로직 검증

    @Test
    public void evenMap() {
        List<Integer> list = IntStream.rangeClosed(1, 100)
                .boxed().collect(Collectors.toList());

        List<Integer> answerlist = IntStream.rangeClosed(1, 100)
                .boxed().collect(Collectors.filtering(i -> i % 2 == 0,Collectors.toList()));

        Flux<Integer> flux = Flux.fromIterable(list);
        Flux<Integer> flux2 = flux.flatMap(i -> Mono.just(i))
                .publishOn(Schedulers.boundedElastic())
                .filter(x-> x%2 == 0)
                .publishOn(Schedulers.parallel())
                .log();

        StepVerifier.create(flux2)
                .expectNextSequence(answerlist)
                .verifyComplete();

    }

    //3. “hello”, “there” 를 순차적으로 publish하여 순서대로 나오는지 검증
    @Test
    public void publishTest() {
        Flux<String> flux = Flux.just("hello","there");
        Flux<String> flux2 = flux.flatMap(i -> Mono.just(i))
                .publishOn(Schedulers.boundedElastic())
                .log();

        StepVerifier.create(flux2)
                .expectNext("hello")
                .expectNext("there")
                .verifyComplete();
    }

    //4. 아래와 같은 객체가 전달될 때 “JOHN”, “JACK” 등 이름이 대문자로 변환되어 출력되는 로직 검증
    //Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678")
    //Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678")
    @Test
    public void replaceTest() {
        Person p1 = new Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678");
        Person p2 = new Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678");

        Flux<Person> flux = Flux.just(p1, p2);
        Flux<String> flux2 = flux.flatMap(i -> {
            return Mono.just(i.name.toUpperCase()+ ", " + i.address + ", " + i.number);
                }).publishOn(Schedulers.boundedElastic())
                .log();

        StepVerifier.create(flux2)
                .expectNext("JOHN, [john@gmail.com](mailto:john@gmail.com), 12345678")
                .expectNext("JACK, [jack@gmail.com](mailto:jack@gmail.com), 12345678")
                .verifyComplete();
    }

    //5. ["Blenders", "Old", "Johnnie"] 와 "[Pride", "Monk", "Walker”]를 압축하여 스트림으로 처리 검증
    @Test
    public void zipTest() {

        Flux<String> flux1 = Flux.just("Blenders", "Old", "Johnnie");
        Flux<String> flux2 = Flux.just("Pride", "Monk", "Walker");

        Flux<String> flux3 = Flux.zip(flux1, flux2)
                .flatMap(i -> Mono.just(i.getT1()+ " "+i.getT2()))
                .log();
        StepVerifier.create(flux3)
                .expectNext("Blenders Pride")
                .expectNext("Old Monk")
                .expectNext("Johnnie Walker")
                .verifyComplete();
    }

    //6.["google", "abc", "fb", "stackoverflow”] 의 문자열 중 5자 이상 되는 문자열만 대문자로 비동기로 치환하여 1번 반복하는 스트림으로 처리하는 로직 검증
    @Test
    public void asyncUpperCaseTest() {

        Flux<String> flux = Flux.just("google", "abc", "fb", "stackoverflow")
                .publishOn(Schedulers.boundedElastic())
                .filter(i -> 5 <= i.length())
                .flatMap(i -> Mono.just(i.toUpperCase()))
                .repeat(1)
                .log();

        StepVerifier.create(flux)
                .expectNext("GOOGLE")
                .expectNext("STACKOVERFLOW")
                //1반복
                .expectNext("GOOGLE")
                .expectNext("STACKOVERFLOW")
                .verifyComplete();

    }

}
