$ ->
    class Enquetes
        @findByAdminKey: (adminKey) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'get'
                dataType: 'json'
        @create: (enquete) ->
            $.ajax
                url: '/'
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: ko.toJSON(enquete)
                dataType: 'json'
        @update: (adminKey, enquete) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: ko.toJSON(enquete)
                dataType: 'json'

    class QuestionOption
        constructor: (qo) ->
            @id = qo.id if qo
            @description = ko.observable(qo.description if qo)
            @valid = ko.computed(@validate)
        validate: =>
            Boolean(@description())

    class Question
        constructor: (q) ->
            @id = q.id if q
            @description = ko.observable(q.description if q)
            @questionType = ko.observable(if q then q.questionType else 'SingleSelection')
            @newQuestionOption = ko.observable()
            @_questionOptions = (if q then q.questionOptions else []).map (o) -> new QuestionOption(o)
            @questionOptions = ko.computed(@rearrangeQuestionOptions)
            @valid = ko.computed(@validate)
        validate: =>
            @description() and @questionOptions().length > 0
        rearrangeQuestionOptions: =>
            if @newQuestionOption()
                @_questionOptions.push(new QuestionOption(description: @newQuestionOption()))
                @newQuestionOption(null)
            @_questionOptions = @_questionOptions.filter (qo) -> qo.valid()

    class Enquete
        constructor: (e) ->
            @title = ko.observable(e.title if e)
            @description = ko.observable(e.description if e)
            @answerLink = ko.observable(e.answerLink if e)
            @questions = ko.observableArray((if e then e.questions else [null]).map (q) -> new Question(q))
            @valid = ko.computed(@validate)
        validate: =>
            @title() and @questions().every (q) -> q.valid()
        addQuestion: =>
            @questions.push(new Question())
        removeQuestion: (q) =>
            @questions.remove(q)

    class App
        constructor: () ->
            @enquete = ko.observable()
            @status = ko.observable(null)
            ko.computed => if @enquete() then @status(null)
        clear: =>
            @enquete(null)
            @status(null)

    ko.applyBindings(app = new App())

    $(document).ajaxStart -> $('.loading').show()
    $(document).ajaxStop  -> $('.loading').fadeOut()
    $(document).ajaxError (e, xhr) -> app.status(xhr.status)
    $('.notification').click -> $(@).fadeOut()

    Sammy ->
        @get  '/', ->
            app.enquete(new Enquete())
        @post '/', ->
            Enquetes.create(app.enquete()).done (d) =>
                @redirect("/admin##{d.adminKey}")
            false
        @get '/admin#:adminKey', ->
            app.clear()
            Enquetes.findByAdminKey(@params.adminKey).done (d) =>
                app.enquete(new Enquete(d.enquete))
        @post '/admin#:adminKey', ->
            Enquetes.update(@params.adminKey, app.enquete()).done (d) =>
                app.enquete(new Enquete(d.enquete))
                app.status(200)
            false
        @get  '', ->
            app.clear()
        @post '', ->
            app.clear()
    .run()
