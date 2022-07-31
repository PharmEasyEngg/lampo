$(document).ready(function() {

	$("html").on('click', ".link-disabled", e => e.preventDefault());

	$("#view-files").on('click', function() {
		let prefix = $('#directory').val();
		$.ajax({
			url: `files?prefix=${prefix}`,
			success: function(data) {
				$("#app-files").dialog({
					autoOpen: false,
					modal: true,
					resizable: false,
					draggable: false,
					close: () => {
						$(".body").removeClass("blur");
						window.location.reload();
					},
					title: "Uploaded Apps",
					width: '50%',
					height: ($(window).height() * 0.4),
				});
				$("#app-files").html(data.map(e => `<a href='${e}'>${e.split('/').at(-1)}</a>`).join("<br/>"));
				$("#app-files").dialog("open");
				$("#ui-id-1").css({ 'text-align': 'center', 'margin-left': '30px' });
				$("#app-files").css({ 'text-align': 'center', 'max-height': ($(window).height() - 200) });
				$(".ui-dialog").css("max-height", ($(window).height() - 200));
				$("div[role=dialog]").css({ 'max-height': '500px', 'top': '100px' });
			}
		}).done(function() {
			$(".body").addClass("blur");
		})
	});

	setInterval(() => {
		$.ajax({
			url: 'refresh', success: data => {
				$(".container-fluid").parent().html(data);
				let links = $("a.card-link").not(".link-disabled");
			}
		})
	}, 5000);

	$(".close-btn").on('click', () => {
		$(".body").removeClass("blur");
		$(".dialog-content").hide();
	});

	$("#file").on('change', function(e) {
		e.preventDefault();

		let formData = new FormData();
		formData.append("file", $('#file')[0].files[0]);
		formData.append("directory", $('#directory').val());

		$.ajax({
			type: 'POST',
			url: 'upload',
			processData: false,
			contentType: false,
			cache: false,
			data: formData,
			beforeSend: function() {
				$(".path").text('');
				$("#popup-title").html('');
				$("#loader").show();
				$('#file').prop('disabled', true);
			},
			success: function(response) {
				$("#popup-title").html('File Uploaded Successfully!');
				$(".path").text(response);
				$(".dialog-content").toggle();
			},
			error: function(response) {
				alert(`error occurred while uploading file with message: ${response}`);
			}
		}).done(function() {
			$("#loader").hide();
			$('#file').prop('disabled', false);
			$(".body").addClass("blur");
		});
	});
});